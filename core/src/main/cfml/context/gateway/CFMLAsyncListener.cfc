
component output="no" {
    public void function onIncomingMessage(struct CFEVENT={}){
        _logEvent('CFMLAysncListener onIncomingMessage -Event Listener');
        
    }
    private void function _handleError(required any catchData, string functionName='unknown'){
        _logEvent('CFMLAysncListener_handleError- Event Listener');
       //writeDump(var="#catchData#",output="console");
    }
    private void function _logEvent(required string text, type="informational"){
        systemOutput(arguments.text,true);
        
    }
}