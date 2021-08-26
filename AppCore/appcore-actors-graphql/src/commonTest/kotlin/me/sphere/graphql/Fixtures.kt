package me.sphere.graphql

import kotlinx.datetime.Instant
import me.sphere.appcore.stubs.Conversation
import me.sphere.appcore.stubs.SphereStub
import me.sphere.models.event.*
import me.sphere.sqldelight.SphereEvent
import me.sphere.sqldelight.SphereEventInvitee

internal object Fixtures {
    val sphere = SphereStub.spheres[0]
    val conversation = Conversation("stub-conversation-0", sphere.id, Instant.fromEpochSeconds(1))

    val sphereEvent = SphereEvent(
        id = "stub-event-01",
        sphereId = sphere.id,
        myInviteeId = "my-invitee-id",
        conversationId = conversation.id,
        chatCardId = conversation.id,
        name = "Stub event",
        creator = SphereEventCreator(
            agentId = "1",
            displayName = "A person",
            displayColor = "FFFFFF",
            displayImageId = null
        ),
        imageFilename = null,
        description = "A description",
        link = null,
        location = SphereEventLocation(
            name = "A location"
        ),
        privacyLevel = SphereEventPrivacyLevel.Sphere,
        inviteeCounts = SphereEventInviteeCounts(
            3, 2, 1, 0
        ),
        startDate = Instant.fromEpochSeconds(1),
        endDate = Instant.fromEpochSeconds(100),
        createdAt = Instant.fromEpochSeconds(-1),
        updatedAt = null,
        viewerCapabilities = SphereEventViewerCapabilities(
            rsvp = SphereEventViewerCapabilities.RSVP(canRSVP = false),
            delete = SphereEventViewerCapabilities.Delete(canDelete = false),
            edit = SphereEventViewerCapabilities.Edit(canEdit = false),
            remind = SphereEventViewerCapabilities.Remind(canRemind = false)
        )
    )

    val myInvitee = SphereEventInvitee(
        id = "my-invitee-id",
        agentId = "my-agent-id",
        sphereEventId = "stub-event-01",
        displayName = "Me",
        displayColor = "000000",
        imageFilename = null,
        remote_rsvp = SphereEventRSVP.Going,
        local_rsvp = null
    )

    val getSphereEventResponse = """
    {
        "data": {
            "viewer": {
                "__typename": "Viewer",
                "sphere": {
                    "__typename": "Sphere",
                    "event": {
                        "__typename": "SphereEvent",
                        "id": "${sphereEvent.id}",
                        "createdAt": "${sphereEvent.createdAt}",
                        "updatedAt": null,
                        "sphereId": "${sphere.id}",
                        "displayImageId": null,
                        "name": "${sphereEvent.name}",
                        "description": "${sphereEvent.description}",
                        "location": {
                            "__typename": "SphereEventLocation",
                            "name": "${sphereEvent.location!!.name}"
                        },
                        "link": null,
                        "startDate": "${sphereEvent.startDate}",
                        "endDate": "${sphereEvent.endDate}",
                        "privacyLevel": "SPHERE",
                        "conversationId": "${conversation.id}",
                        "cardId": "${conversation.id}",
                        "creator": {
                            "__typename": "SphereEventCreator",
                            "agentId": "${sphereEvent.creator!!.agentId}",
                            "displayName": "${sphereEvent.creator!!.displayName}",
                            "displayColor": "${sphereEvent.creator!!.displayColor}",
                            "displayImageId": null
                        },
                        "inviteeCounts": {
                            "__typename": "SphereEventInviteeCounts",
                            "pending": 3,
                            "going": 2,
                            "thinking": 1,
                            "notGoing": 0
                        },
                        "viewerInvitee": {
                            "__typename": "SphereEventInvitee",
                            "id": "${sphereEvent.myInviteeId!!}",
                            "agentId": "${myInvitee.agentId}",
                            "displayName": "${myInvitee.displayName}",
                            "displayColor": "${myInvitee.displayColor}",
                            "displayImageId": null,
                            "rsvp": "GOING"
                        },
                        "viewerCapabilities": {
                            "__typename": "SphereEventCapabilities",
                            "rsvp": {
                                "__typename": "RsvpSphereEventCapability",
                                "permitted": false
                            },
                            "delete": {
                                "__typename": "DeleteSphereEventCapability",
                                "permitted": false
                            },
                            "edit": {
                                "__typename": "EditSphereEventCapability",
                                "permitted": false
                            },
                            "remindToRsvp": {
                                "__typename": "RemindToRSVPSphereEventCapability",
                                "permitted": false
                            }
                        }
                    }
                }
            }
        }
    }
    """

    val goingInvitees = listOf(
        myInvitee,
        SphereEventInvitee(
            id = "another-invitee",
            agentId = "another-agentId",
            sphereEventId = sphereEvent.id,
            displayName = "Another person",
            displayColor = "111111",
            imageFilename = null,
            remote_rsvp = SphereEventRSVP.Going,
            local_rsvp = null
        )
    )

    val inviteesResponse = """
    {
        "data": {
            "viewer": {
                "__typename": "Viewer",
                "sphere": {
                    "__typename": "Sphere",
                    "event": {
                        "__typename": "SphereEvent",
                        "invitees": {
                            "__typename": "SphereEventInvitees",
                            "going": {
                                "__typename": "SphereEventInviteeList",
                                "data": [
                                    {
                                        "__typename": "SphereEventInvitee",
                                        "id": "${sphereEvent.myInviteeId!!}",
                                        "agentId": "${myInvitee.agentId}",
                                        "displayName": "${myInvitee.displayName}",
                                        "displayColor": "${myInvitee.displayColor}",
                                        "displayImageId": null,
                                        "rsvp": "GOING"
                                    },
                                    {
                                        "__typename": "SphereEventInvitee",
                                        "id": "${goingInvitees[1].id}",
                                        "agentId": "${goingInvitees[1].agentId}", 
                                        "displayName": "${goingInvitees[1].displayName}",
                                        "displayColor": "${goingInvitees[1].displayColor}",
                                        "displayImageId": null,
                                        "rsvp": "GOING"
                                    }
                                ]
                            },
                            "pending": {
                                "__typename": "SphereEventInviteeList",
                                "data": []
                            },
                            "notGoing": {
                                "__typename": "SphereEventInviteeList",
                                "data": []
                            },
                            "thinking": {
                                "__typename": "SphereEventInviteeList",
                                "data": []
                            }
                        }
                    }
                }
            }
        }
    }
    """
}
