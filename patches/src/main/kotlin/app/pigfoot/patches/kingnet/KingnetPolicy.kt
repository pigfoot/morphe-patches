package app.pigfoot.patches.kingnet

import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction
import java.security.MessageDigest

internal const val KINGNET_PACKAGE = "chk.kingnet.app"
internal const val KINGNET_VERSION = "4.21.0"
internal const val KINGNET_CODE = "421004"
internal const val KINGNET_SIGNER = "6b7853fdb77120ce00bf4148d059db9f1515c04d"
internal const val KINGNET_ASSET = "assets/www/js/app.js"
internal const val KINGNET_JS_HASH = "e45ae88379fd47c45f804ac6b8d7e0e6b2c1d573fbed165ba2efa66f55b44375"

internal fun kingnetSha256(bytes: ByteArray) = MessageDigest.getInstance("SHA-256")
    .digest(bytes).joinToString("") { "%02x".format(it) }

internal fun requireKingnetTarget(packageName: String?, version: String?, code: String?) {
    require(packageName == KINGNET_PACKAGE && version == KINGNET_VERSION && code == KINGNET_CODE) {
        "Unsupported Kingnet package/version/build"
    }
}

internal fun requireKingnetAsset(bytes: ByteArray) {
    require(kingnetSha256(bytes) == KINGNET_JS_HASH) { "Unsupported or already modified Kingnet JavaScript asset" }
}

internal data class KingnetEdit(val label: String, val before: String, val after: String)

private const val AD_STORE = "\"advertising/setAdStorage\",e.data.Data.Ad2SpecificStyleMemberComponent),"
private const val AD_SETTING = ".a.commit(\"advertising/setAdSetting\",e.data.Data.Settings)),t(e)"

/**
 * Qualified against 4.21.0. Ads: the first-party ad API response is still read by the
 * callers, but is no longer committed to the ad store, so every ad getter stays empty.
 * Marketing: Infobip is never initialized (the personalization path is gated on that
 * flag) and the automatic "come back" reminder is cancelled without being scheduled.
 * Platform messages: the notification-box "platform messages" tab (server-broadcast
 * promotions) is left as an empty list (never null: the page shows a loading skeleton while any
 * list is null) and its unread count is forced to zero.
 * FCM registration, notification handling and every other call stay untouched.
 */
internal val kingnetEdits = listOf(
    KingnetEdit("ad data, first request",
        "1===e.data.Status&&(d.a.commit($AD_STORE" + "d" + AD_SETTING,
        "!1&&(d.a.commit($AD_STORE" + "d" + AD_SETTING),
    KingnetEdit("ad data, second request",
        "1===e.data.Status&&(v.a.commit($AD_STORE" + "v" + AD_SETTING,
        "!1&&(v.a.commit($AD_STORE" + "v" + AD_SETTING),
    KingnetEdit("Infobip initialization",
        "case 0:return MobileMessaging.init({applicationCode:",
        "case 0:return t.abrupt(\"return\",void 0);case 1:return MobileMessaging.init({applicationCode:"),
    KingnetEdit("re-engagement reminder",
        "cordova.plugins.notification.local.cancelAll((function(){var e=this,t=new Date,n=[3,4,5];",
        "cordova.plugins.notification.local.cancelAll((function(){return;var e=this,t=new Date,n=[3,4,5];"),
    KingnetEdit("platform messages, first list",
        "r.mescrollArr[1].listArr=o.data.News,",
        "r.mescrollArr[1].listArr=o.data.News=[],"),
    KingnetEdit("platform messages, first unread count",
        "r.\$store.state.CountUnread=o.data.UnreadCount;",
        "o.data.UnreadCount&&(o.data.UnreadCount.News=0),r.\$store.state.CountUnread=o.data.UnreadCount;"),
    KingnetEdit("platform messages, unread count refresh",
        "200===n.status?(u.a.commit(\"setCountUnread\",n.data),e(n)):t(\"error\")",
        "200===n.status?(n.data&&(n.data.News=0),u.a.commit(\"setCountUnread\",n.data),e(n)):t(\"error\")"),
    KingnetEdit("platform messages, paged list request",
        "b=function(e){return h(\"POST\",\"/NotificationBox/LoadNews\",e,\"communityUser\")}",
        "b=function(e){return Promise.resolve({status:200,data:[]})}")
)

/** All sites are collected/validated on the original before any transformation. */
internal fun patchKingnetJavaScript(bytes: ByteArray): ByteArray {
    val decoder = Charsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
        .onUnmappableCharacter(CodingErrorAction.REPORT)
    val original = decoder.decode(ByteBuffer.wrap(bytes)).toString()
    val sites = kingnetEdits.map { edit ->
        val first = original.indexOf(edit.before)
        require(first >= 0 && original.indexOf(edit.before, first + 1) < 0) {
            "Missing, ambiguous or already modified Kingnet site: ${edit.label}"
        }
        first to edit
    }.sortedBy { it.first }
    require(sites.zipWithNext().all { (a, b) -> a.first + a.second.before.length <= b.first }) {
        "Overlapping Kingnet JavaScript sites"
    }
    val result = StringBuilder(original)
    sites.asReversed().forEach { (index, edit) -> result.replace(index, index + edit.before.length, edit.after) }
    return result.toString().toByteArray(Charsets.UTF_8)
}
