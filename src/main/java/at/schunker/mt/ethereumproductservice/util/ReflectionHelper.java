package at.schunker.mt.ethereumproductservice.util;

public class ReflectionHelper {

    public static String getMethodName(Class className) {
        StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();
        int i = 0;
        int lastClassIndex = 0;
        /*
        for (StackTraceElement ste : stackTraceElements) {
            if (ste.getClassName().equals(className.getName())) {
                lastClassIndex = i;
            }
            i++;
        }
        */
        for (; i < stackTraceElements.length; i++) {
            StackTraceElement ste = stackTraceElements[i];
            if (ste.getClassName().equals(className.getName())) {
                lastClassIndex = i;
            }
        }
        return stackTraceElements[lastClassIndex].getMethodName();
    }

}
