import UserNotifications

// Notification info
var globalPushNotificationToken: String = ""
var globalTappedNotification: String = ""
var globalCompletionHandler: ((UIBackgroundFetchResult) -> Void)? = nil
var globalOnPushHandler: String? = nil
var globalPlugin: PushNotification? = nil

// Thread
let PushNotificationQueue = DispatchQueue(label: "pluginPushNotificationQueue", attributes: .concurrent)
let PushNotificationSemaphore = DispatchSemaphore(value: 0)

// Extension AppDelegate
@objc(AppDelegate) extension AppDelegate {
    
    open override func application(_ application: UIApplication, continue userActivity: NSUserActivity,
                                   restorationHandler: @escaping ([UIUserActivityRestoring]) -> Void) -> Bool {
        guard userActivity.activityType == NSUserActivityTypeBrowsingWeb,
              let incomingURL = userActivity.webpageURL
        else {
            return false
        }
        
        globalTappedNotification = incomingURL.path + ((incomingURL.query != nil) ? ("?" + incomingURL.query!) : "");
        globalPlugin?.notifyOnPush()
        
        return true;
    }
    
    // Catch notification if app launched after user touched on message
    open override func application(_ application: UIApplication, didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey : Any]?) -> Bool {
        if (launchOptions != nil) {
            if let userInfo = launchOptions?[UIApplication.LaunchOptionsKey.remoteNotification] as? [String : Any] {
                globalTappedNotification = self.parseAPSObject(obj: userInfo)
                globalPlugin?.notifyOnPush()
            }
        }
        return super.application(application, didFinishLaunchingWithOptions: launchOptions)
    }
    
    // Register token
    open override func application(_ application: UIApplication, didRegisterForRemoteNotificationsWithDeviceToken deviceToken: Data) {
        let tokenParts = deviceToken.map { data -> String in
            return String(format: "%02.2hhx", data)
        }
        let token = tokenParts.joined()
        globalPushNotificationToken = token
        NSLog("Push token: \(token)")
        PushNotificationSemaphore.signal()
    }
    
    // Token errors handler
    open override func application(_ application: UIApplication, didFailToRegisterForRemoteNotificationsWithError error: Error) {
        NSLog("Failed to register: \(error)")
        PushNotificationSemaphore.signal()
    }
    
    open override func application(_ application: UIApplication, didReceiveRemoteNotification userInfo: [AnyHashable : Any]) {
        NSLog("Got remote notif: \(userInfo)")
        globalTappedNotification = self.parseAPSObject(obj: userInfo as! [String : Any]);
        globalPlugin?.notifyOnPush()
    }
    
    open override func application(_ application: UIApplication, didReceiveRemoteNotification
                              userInfo: [AnyHashable : Any],
                              fetchCompletionHandler completionHandler: @escaping (UIBackgroundFetchResult) -> Void) {
        NSLog("Got remote notif with completion: \(userInfo)")
        globalCompletionHandler = completionHandler
        globalTappedNotification = self.parseAPSObject(obj: userInfo as! [String : Any]);
        globalPlugin?.notifyOnPush()

        DispatchQueue.main.asyncAfter(deadline: .now() + 5.0) {
            if let completionHanlder = globalCompletionHandler {
                NSLog("Notify completion on timer")
                completionHanlder(UIBackgroundFetchResult.newData)
                globalCompletionHandler = nil;
            }
        }
    }
    
    func parseAPSObject(obj: [String : Any]) -> String {
        let aps = obj["aps"] as? [String : Any]
        NSLog("parse aps: \(String(describing: aps))")
        if let str = aps?["category"] as? String {
            NSLog("found aps: \(str)")
            return str
        }
        else if let _ = aps?["content-available"] {
            NSLog("found aps: refresh")
            return "refresh"
        }
        return ""
    }
}

@objc(PushNotification)
class PushNotification: CDVPlugin {
    
    private var pushNotificationCallBackID = ""

    @objc(ready:)
    private func ready(command: CDVInvokedUrlCommand) {
        
        NSLog("Ready notif")
        
        if let completionHanlder = globalCompletionHandler {
            NSLog("Notify completion on tapped")
            completionHanlder(UIBackgroundFetchResult.newData)
            globalCompletionHandler = nil;
        }
        
        self.commandDelegate!.send(
            CDVPluginResult(
                status: CDVCommandStatus_OK
            ),
            callbackId: command.callbackId
        )
    }

    @objc(tapped:)
    private func tapped(command: CDVInvokedUrlCommand) {
        if globalPlugin == nil { // first call, install handler
            NSLog("Installed onPush handler");
            globalPlugin = self
            globalOnPushHandler = command.callbackId
            let pluginResult = CDVPluginResult(status: CDVCommandStatus_NO_RESULT)
            pluginResult?.keepCallback = true
            self.commandDelegate!.send(pluginResult, callbackId: command.callbackId)
            self.notifyOnPush();
        }
        else { //
            if let completionHanlder = globalCompletionHandler {
                NSLog("Notify background completion")
                completionHanlder(UIBackgroundFetchResult.newData)
                globalCompletionHandler = nil;
            }
            let pluginResult = CDVPluginResult(
                status: CDVCommandStatus_OK,
                messageAs: ""
            )
            pluginResult?.keepCallback = false
            self.commandDelegate!.send(pluginResult, callbackId: command.callbackId)
        }
    }
    
    func notifyOnPush() {
        if(globalOnPushHandler != nil && globalTappedNotification != "") {
            let pluginResult = CDVPluginResult(
                status: CDVCommandStatus_OK,
                messageAs: globalTappedNotification
            )
            pluginResult?.keepCallback = true
            self.commandDelegate!.send(pluginResult, callbackId: globalOnPushHandler)
        }
    }
    
    @objc(registration:)
    private func registration(command: CDVInvokedUrlCommand) {
        self.pushNotificationCallBackID = command.callbackId
        self.registerForPushNotifications()
    }
    
    private func registerForPushNotifications() {
        UNUserNotificationCenter.current().requestAuthorization(options: [.alert, .sound, .badge]) {
            (granted, error) in
            NSLog("UserNotifications permission granted: \(granted)")
            
            guard granted else {
                self.pluginError()
                return
            }
            self.getNotificationSettings()
        }
    }
    
    private func getNotificationSettings() {
        UNUserNotificationCenter.current().getNotificationSettings { (settings) in
            NSLog("UserNotifications settings: \(settings)")
            guard settings.authorizationStatus == .authorized else {
                self.pluginError()
                return
            }
            
            DispatchQueue.main.async {
                UIApplication.shared.registerForRemoteNotifications()
            }
            
            PushNotificationQueue.async {
                _ = PushNotificationSemaphore.wait(timeout: .distantFuture)
                if (globalPushNotificationToken.count > 0) {
                    self.pluginReady(token: globalPushNotificationToken)
                } else {
                    self.pluginError()
                }
                PushNotificationSemaphore.signal()
            }
        }
    }
    
    private func pluginReady(token: String = "") {
        self.commandDelegate!.send(
            CDVPluginResult(
                status: CDVCommandStatus_OK,
                messageAs: token
            ),
            callbackId: self.pushNotificationCallBackID
        )
    }
    
    private func pluginError() {
        self.commandDelegate!.send(
            CDVPluginResult(
                status: CDVCommandStatus_ERROR,
                messageAs: "Permission denied"
            ),
            callbackId: self.pushNotificationCallBackID
        )
    }
}
