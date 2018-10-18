component output="no" accessors=true   {
    property name="listener" type="component";
    property name="config" type="struct";
    property name="logfileName" type="string" default="CFMLAsyncWatcher";
    property name="state" type="string" default="stopped";
    property name="eventName" type="string" default="";

    public void function init(struct config ={}, component listener){
        setListener(arguments.listener);
        setConfig(arguments.config);
        _logEvent("Initialize - Event Listener");
        }
    
    public void function start(){
        _logEvent("Start - Event Listener");
        try {
            setState("starting");
            sleep(100);
            _logEvent("Starting - Event Listener");
            setState("running");
            _logEvent('Running - Event Listener');
        } catch (any error) {
            variables.state = "failed";
            _logEvent('Failed Start - Event Listener');
            rethrow();
        }        	
    }
    
   
    public void function stop(){
     _logEvent('Stopping - Event Listener');
     try {
         setState("stopping");
         sleep(100);
        _logEvent('Stopped - Event Listener');
         setState("stopped");

     } catch (any error){
         setState("failed");
         _logEvent('Failed Stop - Event Listener');
         rethrow();
     }

    }
    public void function restart() {
     	systemOutput("restart",true);
        if (variables.state EQ "running") {
            stop();
        }
        setState('restarting');
        _logEvent('Restarting - Event Listener');
        sleep(100);
        start();
        
    }
    public string function getHelper(){
        _logEvent('getHelper - Event Listener');
        return "getHelperData";
    }
    public string function getState(){
        _logEvent('getState - Event Listener[#variables.state#]');
        return variables.state;
    }
   
    public string function sendMessage(struct data = {},string code=""){
        
        _logEvent('sendMessage - Event Listener');
        thread {
        local.event ={
             cfcpath = "",
             method="",
             timeout="",
             OriginatorID = "#createUUID()#",
             CfcMethod = "",
             Data = arguments.data,
            GatewayType="",
            hostName="#getHostName()#"
         };

         _logEvent('sendMessage - Event Listener HostName: [#local.event.hostName#] OriginatorID: [#local.event.OriginatorId#] Available objects [#structKeyList(arguments.data)#] listenerObject:[#isobject(variables.listener)#]');
         getListener().onIncomingMessage(local.event);
        }
        return '';
    }
    private void function _handlerError(required any catchData, string functionName="unknown"){
       systemOutput('handleError',true);
       writeDump(var="#catchData#",output="console");
           }  
    private void function _logEvent(required string text, string type = "informational"){
        systemOutput(arguments.text,true);
       /* writeLog(text=arguments.text,file="#getLogFileName()#",type="#arguments.type#");*/
    }
    private function getHostName() {
        try{ 
            return createObject("java", "java.net.InetAddress").getLocalHost().getHostName(); 
        }
        catch(any e) {
            _handlerError(e,'getHostName');
        }

        var sys = createObject("java", "java.lang.System");

        var hostname = sys.getenv('HOSTNAME');
        if(!isNull(hostname)) return hostname;

        var hostname = sys.getenv('COMPUTERNAME');
        if(!isNull(hostname)) return hostname;

        return 'unknown';

        }
}