if(window.cordova) {
    console.log('Running cordova-' + window.cordova.platformId + '@' + window.cordova.version);
    document.addEventListener('deviceready', preLoadApp, false);
}
else {
    window.setTimeout(() => loadApp("http://localhost:3000"), 500);
}

async function preLoadApp() {
    const appConfigPromise = new Promise((resolve, reject) => {
        CustomConfigParameters.get(resolve, reject, ["targetdomain"]);
    });
    const appConfig = await appConfigPromise;
    console.log("Config:" + JSON.stringify(appConfig));
    void loadApp(appConfig.targetdomain);
}

async function loadApp(targetdomain) {
    // inject mobile script
    console.log("loading mobile script...");
    try {
        await new Promise((resolve, reject) => {
            try {
                const scriptElement = document.createElement("script");
                scriptElement.type = "text/javascript";
                scriptElement.addEventListener("load", (ev) => {
                    resolve({ status: true });
                });
                scriptElement.addEventListener("error", (ev) => {
                    reject(`Failed to load ${scriptElement.src}`);
                });
                scriptElement.src = targetdomain + "/mobile.js";
                document.body.appendChild(scriptElement);
            } catch (error) {
                reject(error);
            }
        });
        // load the web-app in the iframe
        initApp();
    }
    catch(e) { // on error, try reloading after short timeout
        console.log("Loading failed, retrying... err=", e);
        setTimeout(() => {
            console.log("Reloading App...");
            location.reload();
        }, 2500);
    }

}
