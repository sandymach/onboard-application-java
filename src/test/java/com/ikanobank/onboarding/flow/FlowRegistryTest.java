package com.ikanobank.onboarding.flow;
import static org.junit.jupiter.api.Assertions.*; import org.junit.jupiter.api.Test; import com.ikanobank.onboarding.domain.*;
class FlowRegistryTest { @Test void loadsAllSixFlows() throws Exception {FlowRegistry r=new FlowRegistry();assertEquals(6,r.all().size());assertFalse(r.get(Country.SWEDEN,CustomerType.PRIVATE_INDIVIDUAL).steps().isEmpty());} }
