component extends="Gateway"{

    public string function getClass(){
        return ""; // If it is java based it needs to return the java class name else it returns blank string.
    }
    public string function getCFCPath(){
        return "lucee.extension.gateway.CFMLAsyncWatcher";
    }
    public string function getLabel(){
        return "CFML Aysnc Gateway";
    }
    public string function getDescription() {
        return "CFML Aysnc Gateway similar to the other CFML Engines";
    }
    public void function onBeforeUpdate(required string cfcPath
        , required string startUpMode
        , required struct custom) {
        systemOutput('CFMLAsyncWatcher - beforeUpdate',true);
        // Invoked before an error is thrown. Can be used to throw your own error, and/or do logging.
    }
    public void function onBeforeError(cfcatch){
        systemOutput('CFMLAsyncWatcher - onBeforeError',true);
        // Invoked before an error is thrown. Can be used to throw your own error, and/or do logging.
    }
    public string function getListenerCfcMode() {
        systemOutput('CFMLAsyncWatcher - getListenerCfcMode',true);
        return "required"; //Can be one of the following: "none": no listener gets defined "required": defining a listener is required
    }
    public string function getListenerPath(){
        return "lucee.extension.gateway.CFMLAsyncListener";
    }
}