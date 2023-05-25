package org.apache.solr;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.lang.invoke.MethodHandles;

public class StackTracePrinter {

    private static final  org.slf4j.Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    public static void logStackTrace(String prefix)
    {
    StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();
    String outLog="";
    for (StackTraceElement stackTraceElement : stackTraceElements) {
      outLog=outLog+("    at " + stackTraceElement);
    }
    log.info("[MNP] {} Thread.currentThread().getStackTrace():  {}",prefix,outLog);
}

}
