# Debugger Stack Frame Support

## Overview

This PR adds native debugger stack frame support to Lucee, enabling external debuggers (like VS Code DAP debuggers) to inspect the CFML call stack with full scope access. This is a foundational feature for building debugger extensions without requiring Java bytecode instrumentation.

## Motivation

Currently, external CFML debuggers like [luceedebug](https://github.com/softwareCobbler/luceedebug) must use a Java agent to instrument every UDF call at the bytecode level. This approach has significant drawbacks:

1. **Stack overflow risk** - Wrapper methods double stack frame usage, causing StackOverflowError on deep recursion
2. **Performance overhead** - 50-80% overhead in synthetic benchmarks, 25% in real-world apps
3. **Complex configuration** - Requires `-javaagent` JVM args plus JDWP port setup
4. **Memory pressure** - Extra allocations for frame tracking lead to more GC

By adding native frame tracking to Lucee core, debugger extensions can:

- Use simple extension installation instead of JVM agent configuration
- Avoid stack overflow issues entirely
- Achieve lower overhead through optimised internal implementation
- Access frames via a clean public API

## Changes

### PageContextImpl.java

Added `DebuggerFrame` inner class and frame stack management:

```java
// Static flag - checked once at JVM start, zero cost when false
public static final boolean DEBUGGER_ENABLED =
    Caster.toBooleanValue(SystemUtil.getSystemPropOrEnvVar("lucee.debugger.enabled", null), false);

// Frame data structure
public static final class DebuggerFrame {
    public final Local local;
    public final Argument arguments;
    public final Variables variables;
    public final PageSource pageSource;
    public final String functionName;
    private volatile int line;
    // ... constructor and accessors
}

// Frame stack (null when disabled)
private final LinkedList<DebuggerFrame> debuggerFrames = DEBUGGER_ENABLED ? new LinkedList<>() : null;

// Public API
public void pushDebuggerFrame(Local local, Argument arguments, Variables variables,
                              PageSource pageSource, String functionName);
public void popDebuggerFrame();
public DebuggerFrame[] getDebuggerFrames();
public void setDebuggerLine(int line);
public DebuggerFrame getTopmostDebuggerFrame();
```

### UDFImpl.java

Added frame push/pop calls in the `_call` method:

```java
// After pci.addUDF(this):
if (PageContextImpl.DEBUGGER_ENABLED) {
    pci.pushDebuggerFrame(newLocal, newArgs, pc.variablesScope(), ps, getFunctionName());
}

// In finally block, before pci.removeUDF():
if (PageContextImpl.DEBUGGER_ENABLED) {
    pci.popDebuggerFrame();
}
```

### sysprop-envvar.json

Documented the new environment variable:

```json
{
    "sysprop": "lucee.debugger.enabled",
    "envvar": "LUCEE_DEBUGGER_ENABLED",
    "desc": "Enables debugger stack frame capture for external debuggers (e.g., VS Code DAP). When enabled, Lucee maintains a stack of CFML frames with captured scopes (local, arguments, variables) that external debuggers can inspect. Zero cost when disabled. Requires JVM restart to take effect.",
    "category": "debugging",
    "type": "boolean",
    "default": false
}
```

## Usage

### Enabling

Set the environment variable or system property before starting Lucee:

```bash
# Environment variable
export LUCEE_DEBUGGER_ENABLED=true

# Or system property
java -Dlucee.debugger.enabled=true ...
```

### Accessing Frames (from CFML)

```cfml
<cfscript>
pc = getPageContext();
frames = pc.getDebuggerFrames();

for (frame in frames) {
    systemOutput("Function: #frame.functionName#");
    systemOutput("File: #frame.pageSource.getDisplayPath()#");
    systemOutput("Line: #frame.getLine()#");
    systemOutput("Local vars: #structKeyList(frame.local)#");
    systemOutput("Arguments: #structKeyList(frame.arguments)#");
}
</cfscript>
```

### Accessing Frames (from Java/Extension)

```java
PageContextImpl pci = (PageContextImpl) pageContext;
PageContextImpl.DebuggerFrame[] frames = pci.getDebuggerFrames();

if (frames != null) {
    for (DebuggerFrame frame : frames) {
        String funcName = frame.functionName;
        String filePath = frame.pageSource.getDisplayPath();
        int line = frame.getLine();

        // Access scopes
        Scope local = frame.local;
        Argument args = frame.arguments;
        Variables vars = frame.variables;
    }
}
```

## Performance

When `DEBUGGER_ENABLED=false` (default):

- **Zero overhead** - The static final boolean is constant-folded by JIT
- `debuggerFrames` field is `null`, no allocations
- All `if (DEBUGGER_ENABLED)` checks compile away

When `DEBUGGER_ENABLED=true`:

- One `LinkedList.add()` per UDF call
- One `LinkedList.removeLast()` per UDF return
- Frame objects are small (5 reference fields + 1 int)
- No wrapper methods, no extra stack frames

## Design Decisions

### Why No Line Notification?

We considered adding a callback mechanism for line-by-line notification (like `pc.onDebuggerLine(line)`), but decided against it for v1:

| Approach | Pros | Cons |
|----------|------|------|
| **Callback** | Fast in-process breakpoint check | Requires bytecode changes, overhead when not stepping, recompilation needed |
| **JDWP polling** | Already works, zero overhead when not stepping, no Lucee changes | Slightly slower breakpoint check |

Since debuggers like luceedebug already use JDWP for:

- Thread suspend/resume
- Breakpoint management
- Expression evaluation
- Line-level stepping

Adding a callback would duplicate functionality JDWP already provides. The frame API handles what JDWP *can't* do - capturing CFML scopes.

### Recommended Architecture for Debugger Extensions

1. **Use `getDebuggerFrames()`** for stack inspection and scope access
2. **Use JDWP** for breakpoints, stepping, and thread control
3. **No bytecode instrumentation needed** - just enable `lucee.debugger.enabled`

This gives the best of both worlds: native scope access with zero-overhead stepping.

## Future Enhancements

Possible future additions (if needed):

1. **Expression evaluation in frame context** - Evaluate expressions in any frame's scope
2. **FusionDebug integration** - Adapt existing FD code to use this API
3. **Native breakpoint API** - If JDWP overhead becomes an issue

## Testing

A test script is provided at `profiling/test-debugger-frames.cfm` (in luceedebug repo):

```bash
export LUCEE_DEBUGGER_ENABLED=true
ant -buildfile "/path/to/script-runner" \
    -DluceeJar="/path/to/lucee-7.x.jar" \
    -Dwebroot="/path/to/test" \
    -Dexecute="test-debugger-frames.cfm"
```

Expected output shows frames captured with scope contents:

```text
=== Testing Debugger Frame Support ===
DEBUGGER_ENABLED: true
Calling level1() -> level2() -> level3()...
Inside level3() - called from level2 with num=42
  Frame count: 3
  Frame 1: level1 @ /path/test-debugger-frames.cfm
    - local keys: L1VAR
    - arguments keys:
  Frame 2: level2 @ /path/test-debugger-frames.cfm
    - local keys: L2VAR
    - arguments keys: num
  Frame 3: level3 @ /path/test-debugger-frames.cfm
    - local keys: FRAMES,I,FRAME,LOCALVAR
    - arguments keys: msg
=== Test Complete ===
```

## Related

- [luceedebug](https://github.com/softwareCobbler/luceedebug) - VS Code debugger that could use this API
- [FusionDebug integration](lucee/intergral/fusiondebug/) - Existing debugger integration in Lucee
- [ExecutionLog](lucee/runtime/engine/DebugExecutionLog.java) - Related execution logging feature
