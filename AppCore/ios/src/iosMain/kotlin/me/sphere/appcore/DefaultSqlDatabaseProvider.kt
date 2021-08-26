package me.sphere.appcore

import co.touchlab.sqliter.DatabaseConfiguration
import com.squareup.sqldelight.db.SqlDriver
import com.squareup.sqldelight.drivers.native.*
import kotlinx.atomicfu.atomic
import kotlinx.cinterop.*
import me.sphere.sqldelight.*
import me.sphere.appcore.utils.*
import me.sphere.logging.Logger
import platform.Foundation.*

// Track if we have honoured the debug flag in this process already.
private val hasClearedOnceForDebugFlag = atomic(false)

class DatabaseFileCoordinationException(cause: NSError?): RuntimeException("""
    File coordination for database opening has failed.
    [Native Error]
    ${cause?.domain} ${cause?.code}
    ${cause?.localizedDescription}
""".trimIndent())

class DefaultSqlDatabaseProvider(private val logger: Logger, private val taskRunner: BackgroundTaskRunner): SqlDatabaseProvider {
    override fun driverForDatabase(name: String, schema: SqlDriver.Schema, didOpen: ((SqlDriver) -> Unit)?): SqlDriver {
        return coordinateWriting(name, createIfNotExist = true) { url ->
            openDatabase(url, schema).apply {
                didOpen?.invoke(this)
            }
        }
    }

    override fun destroy(name: String): Unit = coordinateWriting(name, createIfNotExist = false) { url ->
        NSFileManager.defaultManager.removeItemAtURL(url, null)
    }

    private fun <R> coordinateWriting(name: String, createIfNotExist: Boolean, action: (NSURL) -> R): R = memScoped {
        // Organization:
        // ${APP_GROUP_CONTAINER_ROOT}/database/${NAME}/${SQLCORE_SCHEMA_HASH}
        val agentDir = agentDir(name)

        if (createIfNotExist) {
            NSFileManager.defaultManager.run {
                if (!fileExistsAtPath(agentDir.path!!)) {
                    createDirectoryAtURL(agentDir, true, null, null)
                }
            }
            logger.info {
                "DB Location: ${agentDir.path!!}" 
            }
        }

        val error = alloc<ObjCObjectVar<NSError?>>()
        var result: R? = null

        error.value = null

        // Open the database under the protection of File Coordination. The whole process also covers initial schema
        // setup, schema migration, SQLCore database invariant checks, and cleaning up stale files.
        //
        // Related: Swift GRDB advices
        // https://github.com/groue/GRDB.swift/blob/master/Documentation/SharingADatabase.md#use-a-database-pool
        val coordinator = NSFileCoordinator(null)
        coordinator.coordinateWritingItemAtURL(agentDir, NSFileCoordinatorWritingForMerging, error.ptr) { url ->
            check(url != null)

            logger.info { "Current SQLCore Schema Hash: $SQLCORE_SCHEMA_HASH" }

            @Suppress("UNCHECKED_CAST")
            NSFileManager.defaultManager.run {
                val urls = contentsOfDirectoryAtURL(url, null, 0, null) as? List<NSURL>

                (urls ?: emptyList())
                    .filter { !(it.lastPathComponent?.startsWith(SQLCORE_SCHEMA_HASH) ?: false) }
                    .forEach {
                        removeItemAtURL(it, null)
                        logger.info { "Deleted stale file at ${it.absoluteString}" }
                    }
            }

            result = action(url)
        }

        return@memScoped result ?: throw DatabaseFileCoordinationException(error.value)
    }

    private fun openDatabase(directoryUrl: NSURL, schema: SqlDriver.Schema): SqlDriver {
        val configuration = DatabaseConfiguration(
            name = SQLCORE_SCHEMA_HASH,
            version = schema.version,
            extendedConfig = DatabaseConfiguration.Extended(
                basePath = directoryUrl.absoluteString!!,
            ),
            create = { connection ->
                wrapConnection(connection) {
                    SqlDatabaseGateway.Schema.create(it)
                }
            },
            upgrade = { connection, oldVersion, newVersion ->
                wrapConnection(connection) {
                    SqlDatabaseGateway.Schema.migrate(it, oldVersion, newVersion)
                }
            }
        )

        return AppCoreSqlDriver(
            NativeSqliteDriver(configuration, maxReaderConnections = 4),
            taskRunner
        )
    }

    private val debugAlwaysClearDatabase: Boolean
        get() = NSUserDefaults.standardUserDefaults.boolForKey("appcore.debug.clearDb")

    private fun appContainerDbDir(): NSURL = NSFileManager.defaultManager.run {
        val appGroupId = NSBundle.mainBundle.objectForInfoDictionaryKey("SphereAppGroupId") as String
        val url = containerURLForSecurityApplicationGroupIdentifier(appGroupId)!!
            .URLByAppendingPathComponent("database", true)!!

        if (debugAlwaysClearDatabase && hasClearedOnceForDebugFlag.compareAndSet(false, true)) {
            logger.info { "Detected `-appcore.debug.clearDb YES`. Deleting all existing databases." }
            removeItemAtURL(url, null)
        }

        return url
    }

    private fun agentDir(name: String): NSURL
        = appContainerDbDir().URLByAppendingPathComponent(name, true)!!
}
