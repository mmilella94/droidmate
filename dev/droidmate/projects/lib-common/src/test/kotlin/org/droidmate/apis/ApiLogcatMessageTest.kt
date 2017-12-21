// DroidMate, an automated execution generator for Android apps.
// Copyright (C) 2012-2016 Konrad Jamrozik
//
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program.  If not, see <http://www.gnu.org/licenses/>.
//
// email: jamrozik@st.cs.uni-saarland.de
// web: www.droidmate.org

package org.droidmate.apis

import org.droidmate.misc.DroidmateException
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.junit.runners.MethodSorters

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@RunWith(JUnit4::class)
class ApiLogcatMessageTest {
    @Test
    fun `Parses simple logcat message payload`() {
        val msg = """
TId: 1;objCls: 'android.webkit.WebView';mthd: 'loadDataWithBaseURL';retCls: 'void';params: 'java.lang.String' null 'java.lang.String' 
'<html><head><style_type="text/css">body_{_font-family:_"default_font";_}' 
'java.lang.String' 'text/html' 
'java.lang.String' 'UTF-8' 
'java.lang.String' null
;stacktrace: 'dalvik.system.VMStack.getThreadStackTrace(Native Method)->dalvik.system.NativeStart.main(Native Method)'
""".trim()
        // Act
        ApiLogcatMessage.from(msg)
    }

    @Test
    fun `Parses no params`() {
        val msg = """
TId: 1;objCls: 'android.webkit.WebView';mthd: 'methd';retCls: 'void';params: ;stacktrace: 'dalvik'
""".trim()
        // Act
        ApiLogcatMessage.from(msg)
    }

    @Test
    fun `Parses param values being empty strings`() {
        val msg1 = """
TId: 1;objCls: 'android.webkit.WebView';mthd: 'loadDataWithBaseURL';retCls: 'void'
;params: 
'java.lang.String' ''
;stacktrace: 'dalvik.system.VMStack.getThreadStackTrace(Native Method)->dalvik.system.NativeStart.main(Native Method)'
""".trim()
        // Act 1
        ApiLogcatMessage.from(msg1)

        val msg2 = """
TId: 1;objCls: 'android.webkit.WebView';mthd: 'loadDataWithBaseURL';retCls: 'void' 
params: 
'java.lang.String' '' 'java.lang.String' '' 'java.lang.String' ''
;stacktrace: 'dalvik.system.VMStack.getThreadStackTrace(Native Method)->dalvik.system.NativeStart.main(Native Method)'
""".trim()
        // Act 2
        ApiLogcatMessage.from(msg2)
    }

    @Test
    fun `Throws exception on duplicate keyword`() {
        val msg = """
TId: 1;objCls: 'android.webkit.WebView';mthd: 'methd';retCls: 'void';params: 'java.lang.String' '<html>';retCls: 'void';stacktrace: 'dalvik'
""".trim()
        try {
            // Act
            ApiLogcatMessage.from(msg)
        } catch (ignored: DroidmateException) {
            return
        }
        assert(false, { "No exception was thrown" })
    }

    @Test
    fun `Parses simple logcat message payload without thread ID`() {
        val msg = """
objCls: 'android.webkit.WebView';mthd: 'loadDataWithBaseURL';retCls: 'void';params: 'java.lang.String' null 'java.lang.String' 
'<html><head><style_type="text/css">body_{_font-family:_"default_font";_}' 
'java.lang.String' 'text/html' 
'java.lang.String' 'UTF-8' 
'java.lang.String' null
;stacktrace: 'dalvik.system.VMStack.getThreadStackTrace(Native Method)->dalvik.system.NativeStart.main(Native Method)'
""".trim()
        // Act
        ApiLogcatMessage.from(msg)
    }

    @Test
    fun `Parses logcat message payload with params with newlines`() {
        val msg = """
TId: 1;objCls: 'android.webkit.WebView';mthd: 'loadDataWithBaseURL';retCls: 'void';params: 'java.lang.String' null 'java.lang.String' 
'<html><head><style_type="text/css">body_{_font-family:_"default_font";_}

@font-face_{
font-family:_"default_font";
src:_url(\\'file:///android_asset/fonts/Roboto-Light.ttf\\');
}

li_{
background:
url(data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAcAAAAHCAYAAADEUlfTAAAAAXNSR0IArs4c6QAAAARnQU1BAACxjwv8YQUAAAAJcEhZcwAADs
IAAA7CARUoSoAAAAAZSURBVBhXY2SYeeY/Aw7ABKWxgkEmycAAAOQhAnJHUU4zAAAAAElFTkSuQmCC)
no-repeat_7px_7px_transparent;
list-style-type:_none;
margin:_0;
padding:_0px_0px_1px_18px;
vertical-align:_middle;
}</style></head><body><h3>Version_1.5.5</h3><ul><li>Automatic_backup/restore</li><li>Search_from_article_view</li>
<li>Black_theme_fixes</li></ul><h3>Version_1.5.2</h3><ul><li>German_and_French_translations</li></ul><h3>Version_1.5.1</h3>
<ul><li>Minor_bug_fixes</li></ul><h3>Version_1.5</h3><ul><li>Android_L_Support</li><li>Android_2.3+_Support</li>
<li>Performance_improvements</li><li>High_resolution_\\'Nearby_mode\\'_photos</li></ul><h3>Version_1.4.4</h3>
<ul><li>Performan_TRUNCATED_TO_1000_CHARS' 
'java.lang.String' 'text/html' 'java.lang.String' 'UTF-8' 'java.lang.String' null
;stacktrace: 'dalvik.system.VMStack.getThreadStackTrace(Native Method)->
java.lang.Thread.getStackTrace(Thread.java:579)->
org.droidmate.monitor.Monitor.getStackTrace(Monitor.java:428)->
org.droidmate.monitor.Monitor.redir_android_webkit_WebView_loadDataWithBaseURL5(Monitor.java:1901)->
java.lang.reflect.Method.invokeNative(Native Method)->
java.lang.reflect.Method.invoke(Method.java:515)->
android.webkit.WebView.loadDataWithBaseURL(WebView.java)->
de.a.a.a.a.a(Unknown Source)->
de.a.a.a.a.b(Unknown Source)->
animaonline.android.wikiexplorer.activities.MainActivity.onCreate(Unknown Source)->
android.app.Activity.performCreate(Activity.java:5231)->
android.app.Instrumentation.callActivityOnCreate(Instrumentation.java:1087)->
android.app.ActivityThread.performLaunchActivity(ActivityThread.java:2148)->
android.app.ActivityThread.handleLaunchActivity(ActivityThread.java:2233)->
android.app.ActivityThread.access\$800(ActivityThread.java:135)->
android.app.ActivityThread\$\H.handleMessage(ActivityThread.java:1196)->
android.os.Handler.dispatchMessage(Handler.java:102)->
android.os.Looper.loop(Looper.java:136)->
android.app.ActivityThread.main(ActivityThread.java:5001)->
java.lang.reflect.Method.invokeNative(Native Method)->
java.lang.reflect.Method.invoke(Method.java:515)->
com.android.internal.os.ZygoteInit\$\MethodAndArgsCaller.run(ZygoteInit.java:785)->
com.android.internal.os.ZygoteInit.main(ZygoteInit.java:601)->
dalvik.system.NativeStart.main(Native Method)'
""".trim()
        // Act
        ApiLogcatMessage.from(msg)
    }

    /**
     * Bug: Parsing message throws StackOverflowError
     * https://hg.st.cs.uni-saarland.de/issues/992
     */
    @Test
    fun `Has no bug #992`() {
        val msg = """
TId: 1;objCls: 'android.webkit.WebView';mthd: 'loadDataWithBaseURL';retCls: 'void'
;params: 
'java.lang.String' 'http://googleads.g.doubleclick.net:80/mads/gma?preqs=0&session_id=16105713006406524141&u_sd=1.3312501&seq_num=1&u_w=600&msid=com.rhmsoft.fm&js=afma-sdk-a-v6.4.1&ms=jFkmuJb6MadPkpSMXdm_1kMB6nknHkWEA9KfxwhbPtLWyUMBlCe4g0T9HsMljPXWLpFlit0NwRQ7Zq7E_byCD0fCNKO6ox9i75EfX761yQzkXdEKHsz1n8L9bEjR7S3BI6zwu4hIxFmM2OwVzrJVrURr0htiqCrh6Y1uR7KTDRoTSGIGjGk3r4VteL0j5qC6XBwJUf1JlbfxXvnQ7EPqDVd6VsCPCmWjYbfSTtasGIvLUjqne3icX-7I8gM9BiDxWWVOlQMQbpPdxgnCIEXX9ZqaGJMEnwAnwvu3EFkfizBi-_MlVUB-6eTBRYOylh6MU7pSYjDXI-kv01Ua3xXx9Q&mv=80430000.com.android.vending&bas_off=0&format=468x60_as&oar=0&net=wi&app_name=11700047.android.com.rhmsoft.fm&hl=en&gnt=0&u_h=905&bas_on=0&ptime=0&u_audio=1&aims=s&adinfo=AI4ME0tAfmigiGBY9LmWrAabrqiUBRQd4CVQkmxAMGrXE663ZH8dwhHk20JqIdWhdZW_4I9CJbX5IMt9MZbRhkBP4p2GeavstM7i-DVWrM8RZwlWhMa3p1841nKooudN7BWneJ_S_ItTYIOPEdEAMd33ybZLFcgiMA0NfWVjNV_bAeDDv_c1QWkMfaeMoDnWyfbLCgC3YI4wXKYggoPfLAuKl7DA1rga-nuKju3UmALpSI6T-pD_A_Q7euurAJG7wR2hi3gXOKMEb6BClXzZXXgnZUUcYdZ7tT5yWaggqJFu86kQ-7wMmLMoHDMx_fZ_TRUNCATED_TO_1000_CHARS' 
'java.lang.String' '<!doctype_html><html><head><meta_charset=\"UTF-8\"><link_href=\"http://fonts.googleapis.com/css?family=Open+Sans:300,400|Slabo+27px:400&lang=en\"_rel=\"stylesheet\"_type=\"text/css\"><style>a{color:#ffffff}body,table,div,ul,li{margin:0;padding:0}body{font-family:\"Slabo_27px\",\"Times_New_Roman\",serif;}</style><script>(function(){window.ss=function(){};}).call(this);(function(){var_c=this;var_e=String.prototype.trim?function(a){return_a.trim()}:function(a){return_a.replace(/^[\\s\\xa0]+|[\\s\\xa0]+\$/g,\"\")},f=function(a,b){return_a<b?-1:a>b?1:0};var_g;a:{var_p=c.navigator;if(p){var_q=p.userAgent;if(q){g=q;break_a}}g=\"\"}var_r=function(a){return-1!=g.indexOf(a)};var_t=r(\"Opera\")||r(\"OPR\"),u=r(\"Trident\")||r(\"MSIE\"),v=r(\"Edge\"),w=r(\"Gecko\")&&!(-1!=g.toLowerCase().indexOf(\"webkit\")&&!r(\"Edge\"))&&!(r(\"Trident\")||r(\"MSIE\"))&&!r(\"Edge\"),x=-1!=g.toLowerCase().indexOf(\"webkit\")&&!r(\"Edge\"),y=function(){var_a=g;if(w)return/rv\\:([^\\);]+)(\\)|;)/.exec(a);if(v)return/Edge\\/([\\d\\.]+)/.exec(a);if(u)return/\\b(?:MSIE|rv_TRUNCATED_TO_1000_CHARS'
;stacktrace: 'dalvik'""".trim()
        // Act
        ApiLogcatMessage.from(msg)
    }

    @Test
    fun `simple array parse`() {
        val msg = """TId: 1;objCls: 'de.upb.testify.runtime.UtilityClass';mthd: 'loggingPoint';retCls: 'void';params: 'java.lang.String' '0' java.lang.Object[] '[public native java.lang.val java.lang.String.concat(java.lang.String)]';stacktrace: 'dalvik.system.VMStack.getThreadStackTrace(Native Method)->java.lang.Thread.getStackTrace(Thread.java:580)->de.upb.testify.runtime.UtilityClass.getStackTrace(UtilityClass.java)->de.upb.testify.runtime.UtilityClass.loggingPoint(UtilityClass.java)->de.upb.testApps.dynamicButton.MainActivity\$1.onClick(MainActivity.java)->android.view.View.performClick(View.java:5204)->android.view.View\$\PerformClick.run(View.java:21153)->android.os.Handler.handleCallback(Handler.java:739)->android.os.Handler.dispatchMessage(Handler.java:95)->android.os.Looper.loop(Looper.java:148)->android.app.ActivityThread.main(ActivityThread.java:5417)->java.lang.reflect.Method.invoke(Native Method)->com.android.internal.os.ZygoteInit\$\MethodAndArgsCaller.run(ZygoteInit.java:726)->com.android.internal.os.ZygoteInit.main(ZygoteInit.java:616)'"""
        ApiLogcatMessage.from(msg)
    }

    @Test
    fun `toStringMatchesFromString`() {
        val msg = """
TId:1;objCls:'android.webkit.WebView';mthd:'loadDataWithBaseURL';retCls:'void';params:'java.lang.String' null 'java.lang.String' '<html><head><style_type="text/css">body_{_font-family:_"default_font";_}

@font-face_{
font-family:_"default_font";
src:_url(\\'file:///android_asset/fonts/Roboto-Light.ttf\\');
}

li_{
background:
url(data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAcAAAAHCAYAAADEUlfTAAAAAXNSR0IArs4c6QAAAARnQU1BAACxjwv8YQUAAAAJcEhZcwAADs
IAAA7CARUoSoAAAAAZSURBVBhXY2SYeeY/Aw7ABKWxgkEmycAAAOQhAnJHUU4zAAAAAElFTkSuQmCC)
no-repeat_7px_7px_transparent;
list-style-type:_none;
margin:_0;
padding:_0px_0px_1px_18px;
vertical-align:_middle;
}</style></head><body><h3>Version_1.5.5</h3><ul><li>Automatic_backup/restore</li><li>Search_from_article_view</li>
<li>Black_theme_fixes</li></ul><h3>Version_1.5.2</h3><ul><li>German_and_French_translations</li></ul><h3>Version_1.5.1</h3>
<ul><li>Minor_bug_fixes</li></ul><h3>Version_1.5</h3><ul><li>Android_L_Support</li><li>Android_2.3+_Support</li>
<li>Performance_improvements</li><li>High_resolution_\\'Nearby_mode\\'_photos</li></ul><h3>Version_1.4.4</h3>
<ul><li>Performan_TRUNCATED_TO_1000_CHARS' 'java.lang.String' 'text/html' 'java.lang.String' 'UTF-8' 'java.lang.String' null;stacktrace:'dalvik.system.VMStack.getThreadStackTrace(Native Method)->
java.lang.Thread.getStackTrace(Thread.java:579)->
org.droidmate.monitor.Monitor.getStackTrace(Monitor.java:428)->
org.droidmate.monitor.Monitor.redir_android_webkit_WebView_loadDataWithBaseURL5(Monitor.java:1901)->
java.lang.reflect.Method.invokeNative(Native Method)->
java.lang.reflect.Method.invoke(Method.java:515)->
android.webkit.WebView.loadDataWithBaseURL(WebView.java)->
de.a.a.a.a.a(Unknown Source)->
de.a.a.a.a.b(Unknown Source)->
animaonline.android.wikiexplorer.activities.MainActivity.onCreate(Unknown Source)->
android.app.Activity.performCreate(Activity.java:5231)->
android.app.Instrumentation.callActivityOnCreate(Instrumentation.java:1087)->
android.app.ActivityThread.performLaunchActivity(ActivityThread.java:2148)->
android.app.ActivityThread.handleLaunchActivity(ActivityThread.java:2233)->
android.app.ActivityThread.access\$800(ActivityThread.java:135)->
android.app.ActivityThread\$\H.handleMessage(ActivityThread.java:1196)->
android.os.Handler.dispatchMessage(Handler.java:102)->
android.os.Looper.loop(Looper.java:136)->
android.app.ActivityThread.main(ActivityThread.java:5001)->
java.lang.reflect.Method.invokeNative(Native Method)->
java.lang.reflect.Method.invoke(Method.java:515)->
com.android.internal.os.ZygoteInit\$\MethodAndArgsCaller.run(ZygoteInit.java:785)->
com.android.internal.os.ZygoteInit.main(ZygoteInit.java:601)->
dalvik.system.NativeStart.main(Native Method)'
""".trim()
        // Act
        val from = ApiLogcatMessage.from(msg)
        val payload = ApiLogcatMessage.toLogcatMessagePayload(from)
        // we need to replace the escaped quotes by normal ones for the comparison
        assert(msg.replace("\\\\'", "'") == payload)
    }

    @Test
    fun `Parses quoted logcat message payload`() {
        val msg = """
TId:692;objCls:'android.content.ContentResolver';mthd:'update';retCls:'int';params:'android.net.Uri' 'content://com.haringeymobile.ukweather.provider/Cities/5' 'android.content.ContentValues' 'TimeThreeHourlyForecast=1511283086950 JsonThreeHourlyForecast={\"cod\":\"200\",\"message\":0.0021,\"cnt\":40,\"list\":[{\"dt\":1511287200,\"main\":{\"temp\":272.49,\"temp_min\":272.244,\"temp_max\":272.49,\"pressure\":999.88,\"sea_level\":1020.4,\"grnd_level\":999.88,\"humidity\":87,\"temp_kf\":0.25},\"weather\":[{\"id\":600,\"main\":\"Snow\",\"description\":\"light snow\",\"icon\":\"13n\"}],\"clouds\":{\"all\":88},\"wind\":{\"speed\":3.48,\"deg\":352.001},\"snow\":{\"3h\":0.12975},\"sys\":{\"pod\":\"n\"},\"dt_txt\":\"2017-11-21 18:00:00\"},{\"dt\":1511298000,\"main\":{\"temp\":271.82,\"temp_min\":271.633,\"temp_max\":271.82,\"pressure\":999.74,\"sea_level\":1020.3,\"grnd_level\":999.74,\"humidity\":88,\"temp_kf\":0.19},\"weather\":[{\"id\":800,\"main\":\"Clear\",\"description\":\"clear sky\",\"icon\":\"01n\"}],\"clouds\":{\"all\":92},\"wind\":{\"speed\":3.47,\"deg\":345.51},\"snow\":{\"3h\":0.02625},\"sys\":{\"pod\":\"n\"},\"dt_txt\":\"2017-11-21 21:00:00\"},{\"dt\":1511308800,\"main\":{\"temp\":271.66,\"temp_min\":271.532,\"temp_max\":271.66,\"pressure\":999.74,\"sea_level\":1020.28,\"grnd_level\":999.74,\"humidity\":88,\"temp_kf_TRUNCATED_TO_1000_CHARS' 'java.lang.String' null 'java.lang.String[]' null;stacktrace:'dalvik.system.VMStack.getThreadStackTrace(Native Method)->java.lang.Thread.getStackTrace(Thread.java:1566)->org.droidmate.monitor.Monitor.getStackTrace(Monitor.java:459)->org.droidmate.monitor.Monitor.redir_android_content_ContentResolver_update_551(Monitor.java:3980)->com.haringeymobile.ukweather.database.c.a(SqlOperation.java)->com.haringeymobile.ukweather.database.GeneralDatabaseService.onHandleIntent(GeneralDatabaseService.java)->android.app.IntentService\$\ServiceHandler.handleMessage(IntentService.java:68)->android.os.Handler.dispatchMessage(Handler.java:102)->android.os.Looper.loop(Looper.java:154)->android.os.HandlerThread.run(HandlerThread.java:61)'
""".trim()
        val from = ApiLogcatMessage.from(msg)
        val payload = ApiLogcatMessage.toLogcatMessagePayload(from)

        assert(msg.replace("\\\\'", "'") == payload)
    }

    @Test
    fun `Parses escaped quoted value`() {
        val msg = """
TId:1;objCls:'android.webkit.WebView';mthd:'methd';retCls:'void';params:'java.lang.String' 'Hello \\'dude\\'';stacktrace:'dalvik'
""".trim()
        val from = ApiLogcatMessage.from(msg)
        val payload = ApiLogcatMessage.toLogcatMessagePayload(from)

        assert(msg.replace("\\\\'", "'") == payload)
    }

    @Test
    fun `Conversion to Kotlin API test`() {
        val msg = """
    TId:1;objCls:'android.app.ActivityThread';mthd:'installContentProviders';retCls:'void';params:'android.content.Context' 'com.srt.appguard.loader.MonitorLoaderApplication@fb649d9' 'java.util.List' '[ContentProviderInfo{name=ch.bailu.aat.gpx className=ch.bailu.aat.providers.GpxProvider}]';stacktrace:'dalvik.system.VMStack.getThreadStackTrace(Native Method)->java.lang.Thread.getStackTrace(Thread.java:1566)->org.droidmate.monitor.Monitor.getStackTrace(Monitor.java:465)->org.droidmate.monitor.Monitor.redir_android_app_ActivityThread_installContentProviders_53(Monitor.java:706)->android.app.ActivityThread.handleBindApplication(ActivityThread.java:5386)->android.app.ActivityThread.-wrap2(ActivityThread.java)->android.app.ActivityThread$\H.handleMessage(ActivityThread.java:1546)->android.os.Handler.dispatchMessage(Handler.java:102)->android.os.Looper.loop(Looper.java:154)->android.app.ActivityThread.main(ActivityThread.java:6121)->java.lang.reflect.Method.invoke(Native Method)->com.android.internal.os.ZygoteInit$\MethodAndArgsCaller.run(ZygoteInit.java:889)->com.android.internal.os.ZygoteInit.main(ZygoteInit.java:779)'
""".trim()
        // Act
        val from = ApiLogcatMessage.from(msg)
        val payload = ApiLogcatMessage.toLogcatMessagePayload(from)

        assert(msg == payload)
    }

}