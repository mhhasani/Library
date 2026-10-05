package com.library.logging;

import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

/**
 * SLF4J/logback has five levels (TRACE, DEBUG, INFO, WARN, ERROR). The sixth level the
 * security baseline asks for — FATAL — is expressed as an ERROR event carrying this marker,
 * so it can be filtered and alerted on separately (the marker is printed in every log line).
 */
public final class LogMarkers {

    public static final Marker FATAL = MarkerFactory.getMarker("FATAL");

    private LogMarkers() {
    }
}
