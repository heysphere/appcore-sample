package me.sphere.test

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SealedClassSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PolymorphicKind
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.SerialKind
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.descriptors.elementNames
import kotlinx.serialization.json.Json
import org.reflections.Reflections
import org.reflections.util.*
import java.io.*
import java.nio.file.Paths
import kotlin.io.path.div
import kotlin.reflect.*
import kotlin.reflect.jvm.jvmName
import kotlin.test.*

/**
 * If you want to override the failing checks, use the `recordSnapshot` Gradle task (next to the `jvmTest` Gradle task).
 * Do this only when you are absolutely confident about your changes not being backward incompatible.
 */
class SchemaCompatTests {
    private val serializerMap: HashMap<KClass<*>, KSerializer<*>> = hashMapOf()
    private val exceptions: MutableSet<KClass<*>> = mutableSetOf()
    private inline fun <reified T> register(element: KSerializer<T>) { serializerMap[T::class] = element }
    private inline fun <reified T: Any> registerException(element: KClass<T>) { exceptions.add(element) }

    private val json = Json { prettyPrint = true }

    @OptIn(ExperimentalSerializationApi::class)
    @Test
    fun checkModelsBackwardCompatibility() {
        val isRecording = System.getenv("SNAPSHOT_RECORD_MODE") == "1"
        val projectDir = Paths.get("").toAbsolutePath()
        val snapshotFile = projectDir / "snapshot.json"
        val current = snapshot()

        if (isRecording) {
            FileWriter(snapshotFile.toFile()).use {
                it.write(json.encodeToString(Snapshot.serializer(), current))
            }
        } else {
            val recorded = FileReader(snapshotFile.toFile()).use {
                json.decodeFromString(Snapshot.serializer(), it.readText())
            }
            val errors = compare(recorded = recorded, current = current)

            if (errors.isNotEmpty()) {
                fail("""
                |Detected backward incompatible changes in Kotlin serializable models:
                ${errors.joinToString("\n") { "|  * $it" }}
                """.trimMargin())
            }
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    private fun snapshot(): Snapshot {
        val reflections = Reflections(
            ConfigurationBuilder()
                .addUrls(ClasspathHelper.forPackage("me.sphere.models"))
                .addUrls(ClasspathHelper.forPackage("me.sphere.sqldelight.operations"))
                .filterInputsBy(
                    FilterBuilder().includePackage("me.sphere.sqldelight.operations", "me.sphere.models")
                )
        )

        val serializersByDiscoveredTypes = reflections
            .getTypesAnnotatedWith(Serializable::class.java)
            .map { it.kotlin }
            .filter { it.visibility == KVisibility.PUBLIC  }
            .associateWith { serializerMap[it] }

        val unassociatedTypes = serializersByDiscoveredTypes
            .filter { (key, value) -> value == null && !exceptions.contains(key) }
            .keys

        if (unassociatedTypes.isNotEmpty()) {
            fail("""
                 |Found no KSerializer<T> for the following types:
                 ${unassociatedTypes.joinToString("\n") { "|  * ${it.qualifiedName ?: it.simpleName ?: it.jvmName}" }}
                 |Have they been added to `SchemaCompatTests.register()`?
                 """.trimMargin())
        }

        val allSerializers = serializersByDiscoveredTypes.values.filterNotNull()
        val types = allSerializers.associate { serializer ->
            val descriptor = serializer.descriptor

            when (val kind = descriptor.kind) {
                is StructureKind.CLASS -> descriptor.serialName to TypeRecord.Class(
                    fields = (0 until descriptor.elementsCount).associate { index ->
                        val fieldName = descriptor.getElementName(index)
                        val fieldDescriptor = descriptor.getElementDescriptor(index)
                        val optionality =
                            if (descriptor.isElementOptional(index)) Optionality.OPTIONAL else Optionality.REQUIRED

                        fieldName to FieldProperties(fieldDescriptor.serialName, optionality)
                    }
                )
                is StructureKind.OBJECT -> descriptor.serialName to TypeRecord.Object
                is PolymorphicKind.SEALED -> {
                    /** Must match [SealedClassSerializer] descriptor format. */
                    val subclassesDescriptor = descriptor.elementNames.indexOf("value")
                        .let(descriptor::getElementDescriptor)
                    descriptor.serialName to TypeRecord.Sealed(
                        subtypes = subclassesDescriptor.elementNames.toSet()
                    )
                }
                is SerialKind.ENUM -> descriptor.serialName to TypeRecord.Enum(
                    cases = (0 until descriptor.elementsCount)
                        .map { index -> descriptor.getElementName(index) }
                        .toSet()
                )
                is PrimitiveKind -> descriptor.serialName to TypeRecord.Primitive(kind.toString())
                else -> error("Unsupported serialization kind ${descriptor.kind} for ${descriptor.serialName}")
            }
        }

        return Snapshot(types)
    }

    private fun compare(recorded: Snapshot, current: Snapshot): List<String> {
        val errors = mutableListOf<String>()

        // Check existing types
        for ((typeName, oldRecord) in recorded.types) {
            // Removing types aren't backward incompatible per se. But fail regardless to avoid the renaming loophole.
            val newRecord = current.types[typeName]
            if (newRecord == null) {
                errors.add("Removed `$typeName`.")
                continue
            }

            // Changing the declaration kind is backward incompatible.
            if (oldRecord::class != newRecord::class) {
                errors.add("Changed `$typeName` from ${oldRecord::class.simpleName} to ${newRecord::class.simpleName}.")
                continue
            }

            val recordErrors = when (oldRecord) {
                is TypeRecord.Class ->
                    visitClass(typeName, oldRecord, newRecord as TypeRecord.Class)
                is TypeRecord.Sealed ->
                    visitSealed(typeName, oldRecord, newRecord as TypeRecord.Sealed)
                is TypeRecord.Enum ->
                    visitEnum(typeName, oldRecord, newRecord as TypeRecord.Enum)
                is TypeRecord.Primitive ->
                    visitPrimitive(typeName, oldRecord, newRecord as TypeRecord.Primitive)
                is TypeRecord.Object ->
                    emptyList()
            }
            errors.addAll(recordErrors)
        }

        return errors
    }

    private fun visitPrimitive(typeName: String, oldRecord: TypeRecord.Primitive, newRecord: TypeRecord.Primitive): List<String> {
        return if (oldRecord.kind != newRecord.kind)
            listOf("Changed primitive kind of `$typeName` from ${oldRecord.kind} to ${newRecord.kind}.")
        else
            emptyList()
    }

    private fun visitEnum(typeName: String, oldRecord: TypeRecord.Enum, newRecord: TypeRecord.Enum): List<String> {
        val errors = mutableListOf<String>()

        // Removing enum case is backward incompatible.
        val removedCases = oldRecord.cases - newRecord.cases
        if (removedCases.isNotEmpty()) {
            for (caseName in removedCases) {
                errors.add("Removed case `$caseName` from enum `$typeName`.")
            }
        }

        return errors
    }

    private fun visitSealed(typeName: String, oldRecord: TypeRecord.Sealed, newRecord: TypeRecord.Sealed): List<String> {
        val errors = mutableListOf<String>()

        // Removing sealed subclasses is backward incompatible.
        val removedSubtypes = oldRecord.subtypes - newRecord.subtypes
        if (removedSubtypes.isNotEmpty()) {
            for (subtypeName in removedSubtypes) {
                errors.add("Removed subclass `$subtypeName` from sealed `$typeName`.")
            }
        }

        return errors
    }

    private fun visitClass(typeName: String, oldRecord: TypeRecord.Class, newRecord: TypeRecord.Class): List<String> {
        val errors = mutableListOf<String>()

        // Removed fields are backward incompatible.
        val removedFieldNames = oldRecord.fields.keys - newRecord.fields.keys
        if (removedFieldNames.isNotEmpty()) {
            for (fieldName in removedFieldNames) {
                errors.add("Removed field `$fieldName` in `$typeName`.")
            }
        }

        // New fields are backward incompatible unless they are optional (has default value).
        val newFieldNames = newRecord.fields.keys - oldRecord.fields.keys
        val newRequiredFields = newRecord.fields
            .filter { (name, record) -> newFieldNames.contains(name) && record.optionality == Optionality.REQUIRED }
        if (newRequiredFields.isNotEmpty()) {
            for (fieldName in newRequiredFields.keys) {
                errors.add("Added non-optional field `$fieldName` in `$typeName`.")
            }
        }

        // Existing fields can be backward incompatible if their optionality or their type has changed.
        val existingFieldNames = newRecord.fields.keys - newFieldNames
        for (fieldName in existingFieldNames) {
            val snapshotField = oldRecord.fields[fieldName]!!
            val currentField = newRecord.fields[fieldName]!!

            if (snapshotField.typeName != currentField.typeName)
                errors.add("Changed type of field `$fieldName` in `$typeName` — from `${snapshotField.typeName}` to `${currentField.typeName}`")

            if (snapshotField.optionality != currentField.optionality)
                errors.add("Changed optionality of field `$fieldName` in `$typeName` — from ${snapshotField.optionality} to ${currentField.optionality}")
        }

        return errors
    }

    @BeforeTest
    fun register() {
//        register(me.sphere.sqldelight.operations.SampleOperation.Input.serializer())
        registerException(me.sphere.models.TaggedString::class)
        registerException(me.sphere.models.Tags.Emoji::class)
    }
}

@Serializable
data class Snapshot(
    val types: Map<String, TypeRecord>
)

@Serializable
sealed class TypeRecord {
    @Serializable
    data class Class(val fields: Map<String, FieldProperties>): TypeRecord()

    @Serializable
    object Object: TypeRecord()

    @Serializable
    data class Sealed(val subtypes: Set<String>): TypeRecord()

    @Serializable
    data class Enum(val cases: Set<String>): TypeRecord()

    @Serializable
    data class Primitive(val kind: String): TypeRecord()
}

@Serializable
data class FieldProperties(
    val typeName: String,
    val optionality: Optionality
)

@Serializable
enum class Optionality {
    OPTIONAL, REQUIRED;
}
