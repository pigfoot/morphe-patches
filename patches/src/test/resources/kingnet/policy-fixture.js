/* fixture: excerpts around the eight qualified sites */
// site
}),e)})));return function(t,n){return e.apply(this,arguments)}}(),m=function(e){return new Promise((function(t,n){h("POST","/AppUser/GetAd",e,d.a.state.loginStatus?"communityUserToken":"communityUser").then((function(e){1===e.data.Status&&(d.a.commit("advertising/setAdStorage",e.data.Data.Ad2SpecificStyleMemberComponent),d.a.commit("advertising/setAdSetting",e.data.Data.Settings)),t(e)})).catch((function(e){n(e)}))}))}},function(e,t,n){"use strict";(function(e){var a=n(405),r=n.n(a);function i(e,t,n,a,r,i,o,s,c,l){"boolean"!=typeof o&&(c=s,s=o,o=!1);var u,d="function"==typeof n?n.options:n;if(e&&e.render&&(d.render=e.render,d.staticRenderFns=e.staticRenderFns,d._compiled=!0,r&&(d.functional=!0)),a&&(d._scopeI
;
// site
}),e)})));return function(t,n){return e.apply(this,arguments)}}(),_=function(e){return new Promise((function(t,n){y("POST","/AppUser/GetAd",e,v.a.state.loginStatus?"communityUserToken":"communityUser").then((function(e){1===e.data.Status&&(v.a.commit("advertising/setAdStorage",e.data.Data.Ad2SpecificStyleMemberComponent),v.a.commit("advertising/setAdSetting",e.data.Data.Settings)),t(e)})).catch((function(e){n(e)}))}))};function C(e,t){var n=Object.keys(e);if(Object.getOwnPropertySymbols){var a=Object.getOwnPropertySymbols(e);t&&(a=a.filter((function(t){return Object.getOwnPropertyDescriptor(e,t).enumerable}))),n.push.apply(n,a)}return n}function w(e){for(var t=1;t<arguments.length;t++){var n=null!=arguments[t
;
// site
d5b683f29",d.a.create(t)).post("people/2/contactInformation/push/".concat(e,"/depersonalize"));var t},f={initialize:function(e){return r()(o.a.mark((function t(){return o.a.wrap((function(t){for(;;)switch(t.prev=t.next){case 0:return MobileMessaging.init({applicationCode:"PLACEHOLDER",geofencingEnabled:"false",ios:{notificationTypes:["alert","badge","sound"],overridingNotificationCenterDelegateDisabled:!0},android:{notificationIcon:"iconss",multipleNotifications:"true",notificationAccentColor:"#15beae"},privacySettings:{userDataPersisting
;
// site
ext=15;break;case 12:e.prev=12,e.t0=e.catch(4);case 15:localStorage.setItem("lastUpdateDate",n);case 16:case"end":return e.stop()}}),e,null,[[4,12]])})));return function(){return e.apply(this,arguments)}}(),u=function(){cordova.plugins.notification.local.cancelAll((function(){var e=this,t=new Date,n=[3,4,5];t.getDate()>15&&(n=[4,5,6]),n.map((function(e){return d(t,e)})).forEach((function(n){var a,r,i=n-t,o=Math.round(i/6e4);cordova.plugins.notification.local.schedule({id:(a=Date.now(),r=new Uint32Array(1),window.crypto.getRandomValues(r),a+Math.floor(r[0]/4294967296*1e10)),title:c.a.t("UserActivity.Title"),text:c.a.t("UserActivity.
;
// site
l.endUpScroll(!1),r.mescrollArr[2].mescroll.endUpScroll(!1),r.mescrollArr[0].listArr=o.data.Notices,r.mescrollArr[1].listArr=o.data.News,r.mescrollArr[2].listArr=o.data.Sharings,(null===o.data.Noti
;
// site
oMore(),(null===o.data.Sharings||o.data.Sharings.length<10)&&r.mescrollArr[2].mescroll.showNoMore(),r.$store.state.CountUnread=o.data.UnreadCount;case 21:a.next=28;break;case 23:a.prev=23,a.t0=a.catch(3),n.
;
// site
{DeviceCode:s.default.prototype.$getLocalStorageControl("code")},"communityUser").then((function(n){200===n.status?(u.a.commit("setCountUnread",n.data),e(n)):t("error")})).catch((function(e){t(e)}))}))},_=function(e){return h("P
;
// site
"communityUser")},g=function(e){return h("POST","/NotificationBox/LoadSharings",e,"communityUser")},b=function(e){return h("POST","/NotificationBox/LoadNews",e,"communityUser")},y=function(){return new Promise((function(e,t){h("POST","/N
;
