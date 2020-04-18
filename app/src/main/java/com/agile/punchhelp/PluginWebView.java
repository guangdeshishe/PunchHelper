package com.agile.punchhelp;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.http.SslError;
import android.os.Build;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.webkit.SslErrorHandler;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.google.gson.Gson;

import java.util.HashMap;

import static com.agile.punchhelp.MainActivity.SUBMIT_FORM_URL;

public class PluginWebView extends WebView {
    private static final String TAG = "PluginWebView";
    private MainActivity mainActivity;
    public final static String MY_PRE_NAME = "PunchHelp";
    private final static String MY_PRE_FORM_KEY = "FormData";

    final String JS_HEAD = "javascript:";
    private static final String HIDE_SUBMIT_BUTTON_JS = "document.getElementById('confirmBtn').style.visibility='hidden';";//隐藏提交按钮js
    private static final String SUBMIT_BUTTON_CLICK_JS = "document.getElementById('confirmBtn').click();";//点击提交按钮js
    private static final String SAVE_FORM_DATA_JS = "document.getElementById('confirmBtn').click();";//点击提交按钮js

    private SharedPreferences mSharedPref;
    private HashMap<String, String> mParams = new HashMap<>();

    public void setMainActivity(MainActivity mainActivity) {
        this.mainActivity = mainActivity;
    }

    public PluginWebView(Context context) {
        super(context);
        init();
    }

    public PluginWebView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        mSharedPref = getContext().getSharedPreferences(MY_PRE_NAME, Context.MODE_PRIVATE);
        initParams();//初始化表单数据
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            setWebContentsDebuggingEnabled(true);
        }
        setBackgroundColor(0);
        setLayerType(LAYER_TYPE_SOFTWARE, null);
        getSettings().setJavaScriptEnabled(true);
        getSettings().setDomStorageEnabled(true);
        WebSettings settings = getSettings();
        settings.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.NORMAL);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        settings.setPluginState(WebSettings.PluginState.ON);
        settings.setJavaScriptCanOpenWindowsAutomatically(true);
        settings.setAllowFileAccess(true);
        settings.setDefaultTextEncodingName("UTF-8");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
//            WebView.enableSlowWholeDocumentDraw();
        }

        setWebChromeClient(new WebChromeClient());


        setWebViewClient(new WebViewClient() {

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return super.shouldOverrideUrlLoading(view, url);
            }

            @Override
            public void onPageFinished(final WebView view, String url) {
                super.onPageFinished(view, url);

                if (url != null && (url.startsWith(SUBMIT_FORM_URL))) {
                    view.evaluateJavascript(JS_HEAD + HIDE_SUBMIT_BUTTON_JS, null);//隐藏提交按钮
                    loadCacheFormData();//加载缓存表单数据
                }
            }

            @Override
            public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
                handler.proceed();
            }
        });

    }

    private void initParams() {
        String formData = mSharedPref.getString(MY_PRE_FORM_KEY, "");
        if (TextUtils.isEmpty(formData)) {
            //初始化默认值
            mParams.put("empCode", "");//员工id
            mParams.put("empName", "");//姓名
            mParams.put("deptName", "");//部门
            mParams.put("workPlace", "");//工作地点
            mParams.put("isReturnWorkPlace", "是");//是否返回工作地
            mParams.put("returnWorkPlaceDate", "");//返回工作日期YYYYMMDD
            mParams.put("notReturnWorkPlaceReason", "");//未返回工作日原因
            mParams.put("isFever", "否");//是否发烧
            mParams.put("feverTemperature", "");//发烧isInHubei温度
            mParams.put("isCough", "否");//是否咳嗽
            mParams.put("isInHubei", "否");//是否在湖北
            mParams.put("hasConfirmedPeopleAround", "否");//周围是否有确诊人员
            mParams.put("confirmedPeopleRelation", "");//确诊人属于
            mParams.put("returnWorkPlaceFourteenDay", "是");//是否超过14天
            mParams.put("isCotenancy", "否");//是否与人合租
            mParams.put("isRoommateReturnFourteenDay", "是");//合租人是否隔离满14天
            mParams.put("roommateSituation", "");//其他合租人情况
            mParams.put("roommateReturnWorkPlaceFourteenDay", "");//所有未满足14天的合租人的具体返回工作地时间
            mParams.put("roommateReturnWorkPlaceDay", "");//所有未返回工作地的合租人的预计返回时间：
            mParams.put("roommateOtherSituation", "");//其他情况说明
            mParams.put("isSharedKitchenBath", "");//1.厨房/洗浴室是否公用？
            mParams.put("isMultiplayerRoom", "");//2.未满足14天的合租人的合租房屋是否属于多人一间？
            mParams.put("isEatingWithRoommate", "");//3.未满足14天的合租人是否与他人共同吃饭？
            mParams.put("isRoommateHealth", "");//4.未满足14天的合租人身体是否健康？

            saveCacheFormData();//保存缓存数据
            return;
        }
        log("获取到缓存JSON数据：" + formData);
        HashMap<String, String> caches = new Gson().fromJson(formData, HashMap.class);
        mParams.putAll(caches);

        log("缓存转换成Map数据：" + mParams);
    }

    /**
     * 获取员工姓名
     *
     * @return
     */
    public String getUserName() {
        return mParams.get("empName");
    }

    /**
     * 保存缓存数据
     */
    private void saveCacheFormData() {
        String paramsJson = new Gson().toJson(mParams);
        log("将要缓存的JSON数据：" + paramsJson);
        mSharedPref.edit().putString(MY_PRE_FORM_KEY, paramsJson).apply();
    }

    private void loadCacheFormData() {//加载缓存的表单数据
        StringBuilder jsBuilder = new StringBuilder(JS_HEAD);
        String empCode = mParams.get("empCode");
        if (!isEmpty(empCode)) {//员工id
            jsBuilder.append("$('#empCode').val('" + mParams.get("empCode") + "');");
        }
        String empName = mParams.get("empName");
        if (!isEmpty(empName)) {//员工姓名
            jsBuilder.append("$('#empName').val('" + mParams.get("empName") + "');");
        }
        String deptName = mParams.get("deptName");
        if (!isEmpty(deptName)) {//具体部门/项目（如DBG_MSD_HW）
            jsBuilder.append("$('#deptName').val('" + mParams.get("deptName") + "');");
        }
        String workPlace = mParams.get("workPlace");
        if (!isEmpty(workPlace)) {//工作地
            jsBuilder.append("$('#workPlace').val('" + mParams.get("workPlace") + "');");
        }
        String isReturnWorkPlace = mParams.get("isReturnWorkPlace");//是否返回工作地
        if (!isEmpty(isReturnWorkPlace)) {
            jsBuilder.append("$(\":radio[name='isReturnWorkPlace'][value='" + isReturnWorkPlace + "']\").prop(\"checked\", \"checked\");$('input[type=radio][name=isReturnWorkPlace]').change();");
        }
        String returnWorkPlaceDate = mParams.get("returnWorkPlaceDate");
        if (!isEmpty(returnWorkPlaceDate)) {//具体返回日期
            jsBuilder.append("$('#returnWorkPlaceDate').val('" + returnWorkPlaceDate + "');");
        }
        String notReturnWorkPlaceReason = mParams.get("notReturnWorkPlaceReason");
        if (!isEmpty(notReturnWorkPlaceReason)) {//未返回工作地原因
            jsBuilder.append("$('#notReturnWorkPlaceReason').val('" + notReturnWorkPlaceReason + "');");
        }
        String isFever = mParams.get("isFever");
        if (!isEmpty(isFever)) {//是否发烧
            jsBuilder.append("$(\":radio[name='isFever'][value='" + isFever + "']\").prop(\"checked\", \"checked\");$('input[type=radio][name=isFever]').change();");
        }
        String feverTemperature = mParams.get("feverTemperature");
        if (!isEmpty(feverTemperature)) {//发烧温度
            jsBuilder.append("$('#feverTemperature').val('" + mParams.get("feverTemperature") + "');");
        }
        String isCough = mParams.get("isCough");
        if (!isEmpty(isCough)) {//是否咳嗽
            jsBuilder.append("$(\":radio[name='isCough'][value='" + isCough + "']\").prop(\"checked\", \"checked\");");
        }
        String isInHubei = mParams.get("isInHubei");
        if (!isEmpty(isInHubei)) {//是否在湖北
            jsBuilder.append("$(\":radio[name='isInHubei'][value='" + isInHubei + "']\").prop(\"checked\", \"checked\");");
        }
        String hasConfirmedPeopleAround = mParams.get("hasConfirmedPeopleAround");
        if (!isEmpty(hasConfirmedPeopleAround)) {//周围是否有确诊人员
            jsBuilder.append("$(\":radio[name='hasConfirmedPeopleAround'][value='" + hasConfirmedPeopleAround + "']\").prop(\"checked\", \"checked\");$('input[type=radio][name=hasConfirmedPeopleAround]').change();");
        }
        String confirmedPeopleRelation = mParams.get("confirmedPeopleRelation");
        if (!isEmpty(confirmedPeopleRelation)) {//确诊人属于
            jsBuilder.append("$(\":radio[name='confirmedPeopleRelation'][value='" + confirmedPeopleRelation + "']\").prop(\"checked\", \"checked\");");
        }
        String returnWorkPlaceFourteenDay = mParams.get("returnWorkPlaceFourteenDay");
        if (!isEmpty(returnWorkPlaceFourteenDay)) {//是否超过14天
            jsBuilder.append("$(\":radio[name='returnWorkPlaceFourteenDay'][value='" + returnWorkPlaceFourteenDay + "']\").prop(\"checked\", \"checked\");");
        }
        String isCotenancy = mParams.get("isCotenancy");
        if (!isEmpty(isCotenancy)) {//是否与人合租
            jsBuilder.append("$(\":radio[name='isCotenancy'][value='" + isCotenancy + "']\").prop(\"checked\", \"checked\");$('input[type=radio][name=isCotenancy]').change();");
        }
        String isRoommateReturnFourteenDay = mParams.get("isRoommateReturnFourteenDay");
        if (!isEmpty(isRoommateReturnFourteenDay)) {//合租人是否隔离满14天
            jsBuilder.append("$(\":radio[name='isRoommateReturnFourteenDay'][value='" + isRoommateReturnFourteenDay + "']\").prop(\"checked\", \"checked\");$('input[type=radio][name=isRoommateReturnFourteenDay]').change();");
        }
        String roommateSituation = mParams.get("roommateSituation");
        if (!isEmpty(roommateSituation)) {//其他合租人情况
            jsBuilder.append("$(\":radio[name='roommateSituation'][value='" + roommateSituation + "']\").prop(\"checked\", \"checked\");$('input[type=radio][name=roommateSituation]').change();");
        }
        String roommateReturnWorkPlaceFourteenDay = mParams.get("roommateReturnWorkPlaceFourteenDay");
        if (!isEmpty(roommateReturnWorkPlaceFourteenDay)) {//填写所有未满足14天的合租人的具体返回工作地时间：
            jsBuilder.append("$('#roommateReturnWorkPlaceFourteenDay').val('" + roommateReturnWorkPlaceFourteenDay + "');");
        }
        String roommateReturnWorkPlaceDay = mParams.get("roommateReturnWorkPlaceDay");
        if (!isEmpty(roommateReturnWorkPlaceDay)) {//填写所有未满足14天的合租人的具体返回工作地时间：
            jsBuilder.append("$('#roommateReturnWorkPlaceDay').val('" + roommateReturnWorkPlaceDay + "');");
        }
        String roommateOtherSituation = mParams.get("roommateOtherSituation");
        if (!isEmpty(roommateOtherSituation)) {//其他情况说明
            jsBuilder.append("$('#roommateOtherSituation').val('" + roommateOtherSituation + "');");
        }
        String isSharedKitchenBath = mParams.get("isSharedKitchenBath");
        if (!isEmpty(isSharedKitchenBath)) {//1.厨房/洗浴室是否公用？
            jsBuilder.append("$(\":radio[name='isSharedKitchenBath'][value='" + isSharedKitchenBath + "']\").prop(\"checked\", \"checked\");");
        }
        String isMultiplayerRoom = mParams.get("isMultiplayerRoom");
        if (!isEmpty(isMultiplayerRoom)) {//2.未满足14天的合租人的合租房屋是否属于多人一间？
            jsBuilder.append("$(\":radio[name='isMultiplayerRoom'][value='" + isMultiplayerRoom + "']\").prop(\"checked\", \"checked\");");
        }
        String isEatingWithRoommate = mParams.get("isEatingWithRoommate");
        if (!isEmpty(isEatingWithRoommate)) {//3.未满足14天的合租人是否与他人共同吃饭？
            jsBuilder.append("$(\":radio[name='isEatingWithRoommate'][value='" + isEatingWithRoommate + "']\").prop(\"checked\", \"checked\");");
        }
        String isRoommateHealth = mParams.get("isRoommateHealth");
        if (!isEmpty(isRoommateHealth)) {//4.未满足14天的合租人身体是否健康？
            jsBuilder.append("$(\":radio[name='isRoommateHealth'][value='" + isRoommateHealth + "']\").prop(\"checked\", \"checked\");");
        }
        String isContactOverseasPeople = mParams.get("isContactOverseasPeople");
        if (!isEmpty(isContactOverseasPeople)) {//近14天是否接触过境外入境人员（交谈、握手、其他肢体接触等）？
            jsBuilder.append("$(\":radio[name='isContactOverseasPeople'][value='" + isContactOverseasPeople + "']\").prop(\"checked\", \"checked\");$('input[type=radio][name=isContactOverseasPeople]').change();");
        }
        String contactOverseasSituation = mParams.get("contactOverseasSituation");
        if (!isEmpty(contactOverseasSituation)) {//接触的时间及境外入境人员所属的国家：
            jsBuilder.append("$('#contactOverseasSituation').val('" + contactOverseasSituation + "');");
        }
        evaluateJavascript(jsBuilder.toString(), null);
    }

    private boolean isEmpty(String value) {
        return TextUtils.isEmpty(value) || "undefined".equals(value);
    }

    private void log(Object log) {
        Log.d(TAG, log.toString());
    }

    private void setSelectedItem(String name, String value) {
        loadUrl(JS_HEAD + "$(\":radio[name='" + name + "'][value='" + value + "']\").prop(\"checked\", \"checked\");");
    }

    public void submit() {//提交表单
        StringBuilder jsBuilder = new StringBuilder(JS_HEAD);
        String sendDataJs = "function getInfo(){return {\n" +
                "                \"deptName\": $('#deptName').val(),\n" +
                "                \"empCode\":  $('#empCode').val(),\n" +
                "                \"empName\": $('#empName').val(),\n" +
                "                \"notReturnWorkPlaceReason\": $('#notReturnWorkPlaceReason').val(),\n" +
                "                \"returnWorkPlaceDate\": $('#returnWorkPlaceDate').val(),\n" +
                "                \"feverTemperature\": $('#feverTemperature').val(),\n" +
                "                \"hasConfirmedPeopleAround\": $(\"input[name='hasConfirmedPeopleAround']:checked\").val(),\n" +
                "                \"isReturnWorkPlace\": $(\"input[name='isReturnWorkPlace']:checked\").val(),\n" +
                "                \"returnWorkPlaceFourteenDay\":  $(\"input[name='returnWorkPlaceFourteenDay']:checked\").val(),\n" +
                "                \"isCough\":  $(\"input[name='isCough']:checked\").val(),\n" +
                "                \"isFever\":  $('input:radio[name=\"isFever\"]:checked').val(),\n" +
                "                \"isInHubei\":   $('input:radio[name=\"isInHubei\"]:checked').val(),\n" +
                "                \"confirmedPeopleRelation\":  $(\"input[name='confirmedPeopleRelation']:checked\").val(),\n" +
                "                \"workPlace\":  $('#workPlace').val(),\n" +
                "                \"isCotenancy\":  $(\"input[name='isCotenancy']:checked\").val(),\n" +
                "                \"isRoommateReturnFourteenDay\":  $(\"input[name='isRoommateReturnFourteenDay']:checked\").val(),\n" +
                "\n" +
                "                \"roommateSituation\":  $(\"input[name='roommateSituation']:checked\").val(),\n" +
                "                \"roommateReturnWorkPlaceFourteenDay\":  $('#roommateReturnWorkPlaceFourteenDay').val(),\n" +
                "                \"roommateReturnWorkPlaceDay\":  $('#roommateReturnWorkPlaceDay').val(),\n" +
                "                \"roommateOtherSituation\":  $('#roommateOtherSituation').val(),\n" +
                "\n" +
                "                \"isSharedKitchenBath\":  $(\"input[name='isSharedKitchenBath']:checked\").val(),\n" +
                "                \"isMultiplayerRoom\":  $(\"input[name='isMultiplayerRoom']:checked\").val(),\n" +
                "                \"isEatingWithRoommate\":  $(\"input[name='isEatingWithRoommate']:checked\").val(),\n" +
                "                \"isRoommateHealth\":  $(\"input[name='isRoommateHealth']:checked\").val(),\n" +
                "\n" +
                "                \"contactOverseasSituation\": $('#contactOverseasSituation').val(),\n" +
                "                \"isContactOverseasPeople\": $(\"input[name='isContactOverseasPeople']:checked\").val()," +
                "            }};getInfo();";
        jsBuilder.append(sendDataJs);
        evaluateJavascript(jsBuilder.toString(), new ValueCallback<String>() {
            @Override
            public void onReceiveValue(String value) {
                log("提交的表单数据：" + value);
                HashMap<String, String> caches = new Gson().fromJson(value, HashMap.class);
                mParams.putAll(caches);
                saveCacheFormData();//将表单数据缓存起来
            }
        });
        //提交表单
        evaluateJavascript(JS_HEAD + SUBMIT_BUTTON_CLICK_JS, null);
    }

    /**
     * 计算纵向范围，用于滚动到底部
     * <p>
     * (non-Javadoc)
     *
     * @see android.webkit.WebView#computeVerticalScrollRange()
     */
    @Override
    protected int computeVerticalScrollRange() {
        int computeVerticalScrollRange = super.computeVerticalScrollRange();
        Log.i(TAG, "computeVerticalScrollRange" + computeVerticalScrollRange);
        return computeVerticalScrollRange;
    }

}
