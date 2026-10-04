package com.disasteralert.cap;
import org.junit.jupiter.api.Test;
import java.io.ByteArrayInputStream;
import static org.junit.jupiter.api.Assertions.*;
class CapParserTest {
 @Test void parsesBasicCap() throws Exception {
   String x="<alert><identifier>A1</identifier><sender>source</sender><sent>2026-01-01T00:00:00Z</sent><status>Actual</status><msgType>Alert</msgType><scope>Public</scope><info><event>Flood</event><headline>Flood warning</headline><description>Move</description><severity>Severe</severity><urgency>Immediate</urgency><certainty>Observed</certainty><area><areaDesc>Zone A</areaDesc><circle>12.9,77.5 10</circle></area><instruction>Move to safety</instruction></info></alert>";
   var p=new CapParser().parse(new ByteArrayInputStream(x.getBytes()));
   assertEquals("A1",p.identifier()); assertEquals("Flood",p.event()); assertEquals("Severe",p.severity());
 }
}
