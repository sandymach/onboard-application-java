package com.ikanobank.onboarding.service;
import static org.junit.jupiter.api.Assertions.*; import java.util.List; import org.junit.jupiter.api.Test; import com.ikanobank.onboarding.domain.*; import com.ikanobank.onboarding.entity.IntegrationResultEntity;
class DecisionServiceTest {
 private IntegrationResultEntity r(CheckOutcome o){IntegrationResultEntity e=new IntegrationResultEntity();e.setOutcome(o);return e;}
 @Test void passesMoveToAgreementCreation(){assertEquals(ApplicationStatus.AGREEMENT_CREATED,new DecisionService().decide(List.of(r(CheckOutcome.PASS))));}
 @Test void reviewWinsOverPass(){assertEquals(ApplicationStatus.MANUAL_REVIEW,new DecisionService().decide(List.of(r(CheckOutcome.PASS),r(CheckOutcome.MANUAL_REVIEW))));}
 @Test void failDeclines(){assertEquals(ApplicationStatus.DECLINED,new DecisionService().decide(List.of(r(CheckOutcome.PASS),r(CheckOutcome.FAIL))));}
}
