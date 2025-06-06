package com.example.skipad;

import android.accessibilityservice.AccessibilityService;
import android.view.accessibility.AccessibilityEvent;
import android.accessibilityservice.GestureDescription;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.SharedPreferences;
import android.graphics.Path;
import android.graphics.Rect;
import android.util.Log;
import android.view.accessibility.AccessibilityNodeInfo;

import java.util.List;

public class MyAccessibilityService extends AccessibilityService {
    private static final String TAG = "MyAccessibilityService";
    private AccessibilityNodeInfo nodeInfo;
    private boolean isGoBack = false;
    private Rect leftTop, rightTop, rightBottom;
    private static final int MAX_RECURSION_DEPTH = 15;

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        nodeInfo = getRootInActiveWindow();
        if (nodeInfo == null || event.getPackageName() == null) return;

        switch (event.getEventType()) {
            case AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED:
                handleNotification(event);
                break;
            case AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED:
                processWindowContentChanged();
                break;
            default:
                break;
        }
    }

    // 处理窗口内容变化
    private void processWindowContentChanged() {
        if (nodeInfo == null) return;
        Rect screenBounds = new Rect();
        nodeInfo.getBoundsInScreen(screenBounds);

        leftTop = createRect(screenBounds.left, screenBounds.top, screenBounds.left + 400, screenBounds.top + 340);
        rightTop = createRect(screenBounds.right - 400, screenBounds.top, screenBounds.right, screenBounds.top + 340);
        rightBottom = createRect(screenBounds.right - 400, screenBounds.bottom - 340, screenBounds.right, screenBounds.bottom);

        CharSequence pkgName = nodeInfo.getPackageName();
        if (pkgName == null) return;
        String packageName = pkgName.toString();

        if (packageName.equals("com.tencent.mm")) {
            handleWeChatRedEnvelope();
        } else {
            // 忽略安卓设置和MIUI桌面的变化
            if (packageName.matches("com\\.android\\..*") || packageName.equals("com.miui.home")) {
                return;
            }
            skipAd(packageName);
        }
    }

    // 处理微信红包
    private void handleWeChatRedEnvelope() {
        if (nodeInfo == null) return;
        SharedPreferences sharedPreferences = getSharedPreferences("time_num", MODE_PRIVATE);
        boolean isGrabRedEnvelopeEnabled = sharedPreferences.getBoolean("open_grab", true);
        if (!isGrabRedEnvelopeEnabled) return;

        List<AccessibilityNodeInfo> titleNodes = nodeInfo.findAccessibilityNodeInfosByViewId("com.tencent.mm:id/obn");
        List<AccessibilityNodeInfo> openNodes = nodeInfo.findAccessibilityNodeInfosByViewId("com.tencent.mm:id/j6g");
        List<AccessibilityNodeInfo> closeNodes = nodeInfo.findAccessibilityNodeInfosByViewId("com.tencent.mm:id/j6f");
        List<AccessibilityNodeInfo> backNodes = nodeInfo.findAccessibilityNodeInfosByViewId("com.tencent.mm:id/nnc");
        List<AccessibilityNodeInfo> moreNodes = nodeInfo.findAccessibilityNodeInfosByViewId("com.tencent.mm:id/nnk");
        List<AccessibilityNodeInfo> yuanNodes = nodeInfo.findAccessibilityNodeInfosByViewId("com.tencent.mm:id/iyz");
        List<AccessibilityNodeInfo> smsListNodes = nodeInfo.findAccessibilityNodeInfosByViewId("com.tencent.mm:id/cj1");

        // 群聊界面
        if (titleNodes != null && !titleNodes.isEmpty() && titleNodes.get(0) != null) {
            CharSequence titleText = titleNodes.get(0).getText();
            if (titleText != null && titleText.toString().matches(".*\\(\\d+\\).*")) {
                List<AccessibilityNodeInfo> smsNodes = nodeInfo.findAccessibilityNodeInfosByViewId("com.tencent.mm:id/bkg");
                if (smsNodes != null && !smsNodes.isEmpty()) {
                    for (AccessibilityNodeInfo smsNode : smsNodes) {
                        if (smsNode == null) continue;
                        List<AccessibilityNodeInfo> redPacketTexts = smsNode.findAccessibilityNodeInfosByText("微信红包");
                        List<AccessibilityNodeInfo> iconNodes = smsNode.findAccessibilityNodeInfosByViewId("com.tencent.mm:id/a3o");
                        List<AccessibilityNodeInfo> stateNodes = smsNode.findAccessibilityNodeInfosByViewId("com.tencent.mm:id/a3m");
                        if (redPacketTexts != null && !redPacketTexts.isEmpty() &&
                                iconNodes != null && !iconNodes.isEmpty() &&
                                (stateNodes == null || stateNodes.isEmpty()) &&
                                smsNode.isClickable()) { // 领取状态id，已领取、专属、过期
                            Log.d(TAG, "handleWeChatRedEnvelope: 群聊界面，找到未领取红包消息");
                            smsNode.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                            break;
                        }
                    }
                }
            }
        }
        // 开红包界面
        else if (openNodes != null && !openNodes.isEmpty() && openNodes.get(0) != null &&
                closeNodes != null && !closeNodes.isEmpty() && closeNodes.get(0) != null) {
            AccessibilityNodeInfo openNode = openNodes.get(0);
            AccessibilityNodeInfo closeNode = closeNodes.get(0);
            CharSequence openDesc = openNode.getContentDescription();
            CharSequence closeDesc = closeNode.getContentDescription();
            if (openNode.isClickable() && openDesc != null && openDesc.toString().equals("开") &&
                    closeNode.isClickable() && closeDesc != null && closeDesc.toString().equals("关闭")) {
                Log.d(TAG, "handleWeChatRedEnvelope: 开红包界面，找到开、关按钮");
                int delay_time = sharedPreferences.getInt("delay_time", 500);
                try {
                    Thread.sleep(delay_time);
                } catch (InterruptedException e) {
                    Log.e(TAG, "Sleep interrupted", e);
                }
                openNode.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                isGoBack = true;
            }
        }
        // 开后界面
        else if (backNodes != null && !backNodes.isEmpty() && backNodes.get(0) != null &&
                moreNodes != null && !moreNodes.isEmpty() && moreNodes.get(0) != null &&
                yuanNodes != null && !yuanNodes.isEmpty() && yuanNodes.get(0) != null) {
            AccessibilityNodeInfo backNode = backNodes.get(0);
            AccessibilityNodeInfo moreNode = moreNodes.get(0);
            AccessibilityNodeInfo yuanNode = yuanNodes.get(0);
            CharSequence backDesc = backNode.getContentDescription();
            CharSequence moreDesc = moreNode.getContentDescription();
            CharSequence yuanText = yuanNode.getText();
            if (backNode.isClickable() && backDesc != null && backDesc.toString().equals("返回") &&
                    moreNode.isClickable() && moreDesc != null && moreDesc.toString().equals("更多") &&
                    yuanText != null && yuanText.toString().equals("元")) {
                if (isGoBack) {
                    Log.d(TAG, "handleWeChatRedEnvelope: 开红包后界面");
                    backNode.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                    isGoBack = false;
                }
            }
        }
        // 消息列表界面
        else if (smsListNodes != null && !smsListNodes.isEmpty()) {
            for (AccessibilityNodeInfo smsListNode : smsListNodes) {
                if (smsListNode == null) continue;
                List<AccessibilityNodeInfo> redBaoSmsNodes = smsListNode.findAccessibilityNodeInfosByViewId("com.tencent.mm:id/ht5");
                if (redBaoSmsNodes != null && !redBaoSmsNodes.isEmpty() && redBaoSmsNodes.get(0) != null) {
                    CharSequence smsText = redBaoSmsNodes.get(0).getText();
                    if (smsText != null && smsText.toString().contains("[微信红包] ") &&
                            smsListNode.isClickable() &&
                            ((smsListNode.findAccessibilityNodeInfosByViewId("com.tencent.mm:id/o_u") != null &&
                                    !smsListNode.findAccessibilityNodeInfosByViewId("com.tencent.mm:id/o_u").isEmpty()) ||
                                    (smsListNode.findAccessibilityNodeInfosByViewId("com.tencent.mm:id/a_h") != null &&
                                            !smsListNode.findAccessibilityNodeInfosByViewId("com.tencent.mm:id/a_h").isEmpty()))) {
                        smsListNode.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                        break;
                    }
                }
            }
        }
    }

    // 通过文本跳过广告
    private void skipAd(String packageName) {
        if (nodeInfo == null) return;
        List<AccessibilityNodeInfo> textNodeList = nodeInfo.findAccessibilityNodeInfosByText("跳过");
        Rect rectSkip = new Rect();
        if (textNodeList == null || textNodeList.isEmpty() || textNodeList.get(0) == null) {
            // 通过ID跳过广告
            String skipBtnId;
            switch (packageName) {
                case "com.baidu.netdisk":
                    skipBtnId = "com.baidu.netdisk:id/countdown";
                    break;
                case "com.cainiao.wireless":
                    skipBtnId = "com.cainiao.wireless:id/tt_splash_skip_btn";
                    break;
                case "com.MobileTicket":
                    skipBtnId = "com.MobileTicket:id/tv_skip";
                    break;
                case "com.netease.cloudmusic":
                    skipBtnId = "com.netease.cloudmusic:id/skipBtn";
                    break;
                case "com.xiaomi.shop":
                    skipBtnId = "com.xiaomi.shop:id/skip";
                    break;
                case "com.youdao.dict":
                    skipBtnId = "com.youdao.dict:id/skip_bottom_view";
                    List<AccessibilityNodeInfo> youdaoIdNodeList = nodeInfo.findAccessibilityNodeInfosByViewId(skipBtnId);
                    if (youdaoIdNodeList == null || youdaoIdNodeList.isEmpty()) {
                        skipBtnId = "com.youdao.dict:id/iv_close_bt";
                    }
                    break;
                default:
                    skipBtnId = "skipBtnId_Not_Found";
                    break;
            }
            List<AccessibilityNodeInfo> idNodeList = nodeInfo.findAccessibilityNodeInfosByViewId(skipBtnId);
            if (idNodeList == null || idNodeList.isEmpty() || idNodeList.get(0) == null) {
                // 根据节点查找跳过按钮
                AccessibilityNodeInfo traversalNode = findClickableViewInArea(nodeInfo, 0);
                if (traversalNode != null) {
                    if ((nodeInfo.findAccessibilityNodeInfosByText("详情页或第三方应用") != null && !nodeInfo.findAccessibilityNodeInfosByText("详情页或第三方应用").isEmpty())
                            || (nodeInfo.findAccessibilityNodeInfosByText("扭一扭") != null && !nodeInfo.findAccessibilityNodeInfosByText("扭一扭").isEmpty())
                            || (nodeInfo.findAccessibilityNodeInfosByText("扭动手机") != null && !nodeInfo.findAccessibilityNodeInfosByText("扭动手机").isEmpty())
                            || (nodeInfo.findAccessibilityNodeInfosByText("翻转手机") != null && !nodeInfo.findAccessibilityNodeInfosByText("翻转手机").isEmpty())
                            || (nodeInfo.findAccessibilityNodeInfosByText("向上滑动") != null && !nodeInfo.findAccessibilityNodeInfosByText("向上滑动").isEmpty())
                            || (nodeInfo.findAccessibilityNodeInfosByText("滑一滑 或 扭一扭") != null && !nodeInfo.findAccessibilityNodeInfosByText("滑一滑 或 扭一扭").isEmpty())
                            || (nodeInfo.findAccessibilityNodeInfosByText("摇一摇 或 点击图标") != null && !nodeInfo.findAccessibilityNodeInfosByText("摇一摇 或 点击图标").isEmpty())
                            || (nodeInfo.findAccessibilityNodeInfosByText("转动手机或点击图标") != null && !nodeInfo.findAccessibilityNodeInfosByText("转动手机或点击图标").isEmpty())
                            || (nodeInfo.findAccessibilityNodeInfosByText("扭动或点击立即下载") != null && !nodeInfo.findAccessibilityNodeInfosByText("扭动或点击立即下载").isEmpty())
                            || (nodeInfo.findAccessibilityNodeInfosByText("下载或跳转第三方应用") != null && !nodeInfo.findAccessibilityNodeInfosByText("下载或跳转第三方应用").isEmpty())
                            || (nodeInfo.findAccessibilityNodeInfosByText("向上滑动或点击按钮查看") != null && !nodeInfo.findAccessibilityNodeInfosByText("向上滑动或点击按钮查看").isEmpty())
                            || (nodeInfo.findAccessibilityNodeInfosByText("向上滑动查看") != null && !nodeInfo.findAccessibilityNodeInfosByText("向上滑动查看").isEmpty())
                            || (nodeInfo.findAccessibilityNodeInfosByText("点击查看详情") != null && !nodeInfo.findAccessibilityNodeInfosByText("点击查看详情").isEmpty())
                            || (nodeInfo.findAccessibilityNodeInfosByText("点击跳转详情页面") != null && !nodeInfo.findAccessibilityNodeInfosByText("点击跳转详情页面").isEmpty())
                            || (nodeInfo.findAccessibilityNodeInfosByText("查看详情或跳转第三方应用") != null && !nodeInfo.findAccessibilityNodeInfosByText("查看详情或跳转第三方应用").isEmpty())
                            || (nodeInfo.findAccessibilityNodeInfosByText("上滑或点击跳转至详情页") != null && !nodeInfo.findAccessibilityNodeInfosByText("上滑或点击跳转至详情页").isEmpty())
                            || (nodeInfo.findAccessibilityNodeInfosByText("跳转详情页面或第三方应用") != null && !nodeInfo.findAccessibilityNodeInfosByText("跳转详情页面或第三方应用").isEmpty())) {
                        Log.d(TAG, "skipAd: 遍历找到跳过位置(控件点击)");
                        traversalNode.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                    }
                }
            } else {
                AccessibilityNodeInfo idNode = idNodeList.get(0);
                if (idNode != null) {
                    idNode.getBoundsInScreen(rectSkip);
                    if (rectInArea(rectSkip)) {
                        if (idNode.isClickable()) {
                            idNode.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                            Log.d(TAG, "skipAd: 找到跳过id(控件点击)");
                        } else {
                            clickNode(rectSkip);
                            Log.d(TAG, "skipAd: 找到跳过id(坐标点击)");
                        }
                    }
                }
            }
        } else {
            AccessibilityNodeInfo textNode = textNodeList.get(0);
            if (textNode != null) {
                textNode.getBoundsInScreen(rectSkip);
                if (rectInArea(rectSkip)) {
                    if (textNode.isClickable()) {
                        textNode.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                        Log.d(TAG, "skipAd: 找到跳过文本(控件点击)");
                    } else {
                        clickNode(rectSkip);
                        Log.d(TAG, "skipAd: 找到跳过文本(坐标点击)");
                    }
                }
            }
        }
    }

    private boolean rectInArea(Rect rectSkip) {
        return (leftTop != null && leftTop.contains(rectSkip)) ||
                (rightTop != null && rightTop.contains(rectSkip)) ||
                (rightBottom != null && rightBottom.contains(rectSkip));
    }

    // 遍历所有节点及其子节点, 查找可点击的控件
    private AccessibilityNodeInfo findClickableViewInArea(AccessibilityNodeInfo node, int depth) {
        if (node == null || depth > MAX_RECURSION_DEPTH) return null;

        // 检查节点是否为 android.view.View 并且可点击
        CharSequence className = node.getClassName();
        if (node.isClickable() && className != null && "android.view.View".contentEquals(className)) {
            // 获取节点在屏幕中的位置
            Rect rectSkip = new Rect();
            node.getBoundsInScreen(rectSkip);
            // 检查节点是否在指定区域内
            if (rectInArea(rectSkip)) {
                // 返回符合条件的节点和其位置
                return node;
            }
        }

        // 遍历子节点
        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (child != null) {
                AccessibilityNodeInfo result = findClickableViewInArea(child, depth + 1);
                if (result != null) {
                    return result;
                }
            }
        }

        return null;
    }

    // 处理通知栏消息
    private void handleNotification(AccessibilityEvent event) {
        List<CharSequence> texts = event.getText();
        if (texts == null) return;
        for (CharSequence text : texts) {
            if (text != null && text.toString().contains("[微信红包]") && event.getParcelableData() instanceof Notification) {
                Notification notification = (Notification) event.getParcelableData();
                if (notification != null) {
                    PendingIntent pendingIntent = notification.contentIntent;
                    if (pendingIntent != null) {
                        try {
                            pendingIntent.send();
                        } catch (PendingIntent.CanceledException e) {
                            Log.d(TAG, "PendingIntent Canceled");
                        }
                    }
                }
            }
        }
    }

    // 点击某个坐标位置
    private void clickAction(int x, int y) {
        if (x < 0 || y < 0) return;
        GestureDescription.Builder clickBuilder = new GestureDescription.Builder();
        Path clickPath = new Path();
        clickPath.moveTo(x, y);
        GestureDescription.StrokeDescription clickStroke = new GestureDescription.StrokeDescription(clickPath, 0L, 10L);
        clickBuilder.addStroke(clickStroke);
        dispatchGesture(clickBuilder.build(), null, null);
    }

    // 创建Rect
    private Rect createRect(int left, int top, int right, int bottom) {
        return new Rect(left, top, right, bottom);
    }

    // 点击节点
    private void clickNode(Rect rect) {
        if (rect != null) {
            clickAction(rect.centerX(), rect.centerY());
        }
    }

    @Override
    public void onInterrupt() {
        Log.e(TAG, "onInterrupt: Service interrupted");
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        Log.d(TAG, "Accessibility Service Connected");
    }
}