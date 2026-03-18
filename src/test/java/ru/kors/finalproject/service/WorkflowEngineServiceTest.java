package ru.kors.finalproject.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.kors.finalproject.entity.*;
import ru.kors.finalproject.repository.*;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/**
 * Unit tests for WorkflowEngineService state-machine transition rules.
 *
 * Each workflow (Registration, FX, Mobility, GradeChange, Request, ClearanceCheckpoint)
 * is tested for:
 *   - every valid "happy path" transition (should NOT throw)
 *   - representative invalid transitions (should throw IllegalStateException)
 */
@ExtendWith(MockitoExtension.class)
class WorkflowEngineServiceTest {

    // Repositories are required for construction — mocked but not used in the pure-logic tests.
    @Mock private StudentRequestRepository studentRequestRepository;
    @Mock private FxRegistrationRepository fxRegistrationRepository;
    @Mock private MobilityApplicationRepository mobilityApplicationRepository;
    @Mock private ClearanceSheetRepository clearanceSheetRepository;
    @Mock private GradeChangeRequestRepository gradeChangeRequestRepository;
    @Mock private RegistrationRepository registrationRepository;
    @Mock private RegistrationWindowRepository registrationWindowRepository;
    @Mock private AuditLogRepository auditLogRepository;

    @InjectMocks
    private WorkflowEngineService engine;

    // =========================================================================
    // Registration transitions
    // =========================================================================

    @Nested
    @DisplayName("Registration status transitions")
    class RegistrationTransitions {

        @Test void draft_to_submitted()     { assertDoesNotThrow(() -> engine.assertRegistrationTransition(Registration.RegistrationStatus.DRAFT, Registration.RegistrationStatus.SUBMITTED)); }
        @Test void submitted_to_confirmed() { assertDoesNotThrow(() -> engine.assertRegistrationTransition(Registration.RegistrationStatus.SUBMITTED, Registration.RegistrationStatus.CONFIRMED)); }
        @Test void submitted_to_dropped()   { assertDoesNotThrow(() -> engine.assertRegistrationTransition(Registration.RegistrationStatus.SUBMITTED, Registration.RegistrationStatus.DROPPED)); }
        @Test void confirmed_to_dropped()   { assertDoesNotThrow(() -> engine.assertRegistrationTransition(Registration.RegistrationStatus.CONFIRMED, Registration.RegistrationStatus.DROPPED)); }
        @Test void dropped_to_submitted()   { assertDoesNotThrow(() -> engine.assertRegistrationTransition(Registration.RegistrationStatus.DROPPED, Registration.RegistrationStatus.SUBMITTED)); }
        @Test void dropped_to_confirmed()   { assertDoesNotThrow(() -> engine.assertRegistrationTransition(Registration.RegistrationStatus.DROPPED, Registration.RegistrationStatus.CONFIRMED)); }

        @Test
        @DisplayName("DRAFT → CONFIRMED is invalid (must go through SUBMITTED first)")
        void draft_to_confirmed_invalid() {
            assertThatThrownBy(() -> engine.assertRegistrationTransition(
                    Registration.RegistrationStatus.DRAFT, Registration.RegistrationStatus.CONFIRMED))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("DRAFT");
        }

        @Test
        @DisplayName("CONFIRMED → SUBMITTED is invalid")
        void confirmed_to_submitted_invalid() {
            assertThatThrownBy(() -> engine.assertRegistrationTransition(
                    Registration.RegistrationStatus.CONFIRMED, Registration.RegistrationStatus.SUBMITTED))
                    .isInstanceOf(IllegalStateException.class);
        }
    }

    // =========================================================================
    // FX transitions
    // =========================================================================

    @Nested
    @DisplayName("FX registration status transitions")
    class FxTransitions {

        @Test void pending_to_approved()    { assertDoesNotThrow(() -> engine.assertFxTransition(FxRegistration.FxStatus.PENDING, FxRegistration.FxStatus.APPROVED)); }
        @Test void approved_to_paid()       { assertDoesNotThrow(() -> engine.assertFxTransition(FxRegistration.FxStatus.APPROVED, FxRegistration.FxStatus.PAID)); }
        @Test void approved_to_confirmed()  { assertDoesNotThrow(() -> engine.assertFxTransition(FxRegistration.FxStatus.APPROVED, FxRegistration.FxStatus.CONFIRMED)); }
        @Test void paid_to_confirmed()      { assertDoesNotThrow(() -> engine.assertFxTransition(FxRegistration.FxStatus.PAID, FxRegistration.FxStatus.CONFIRMED)); }

        @Test
        @DisplayName("CONFIRMED is a terminal state — no further transitions allowed")
        void confirmed_terminal() {
            for (FxRegistration.FxStatus next : FxRegistration.FxStatus.values()) {
                if (next != FxRegistration.FxStatus.CONFIRMED) {
                    assertThatThrownBy(() -> engine.assertFxTransition(FxRegistration.FxStatus.CONFIRMED, next))
                            .isInstanceOf(IllegalStateException.class);
                }
            }
        }

        @Test
        @DisplayName("PENDING → CONFIRMED skips required steps and is invalid")
        void pending_to_confirmed_invalid() {
            assertThatThrownBy(() -> engine.assertFxTransition(FxRegistration.FxStatus.PENDING, FxRegistration.FxStatus.CONFIRMED))
                    .isInstanceOf(IllegalStateException.class);
        }
    }

    // =========================================================================
    // Mobility transitions
    // =========================================================================

    @Nested
    @DisplayName("Mobility application status transitions")
    class MobilityTransitions {

        @Test void draft_to_submitted()     { assertDoesNotThrow(() -> engine.assertMobilityTransition(MobilityApplication.MobilityStatus.DRAFT, MobilityApplication.MobilityStatus.SUBMITTED)); }
        @Test void submitted_to_in_review() { assertDoesNotThrow(() -> engine.assertMobilityTransition(MobilityApplication.MobilityStatus.SUBMITTED, MobilityApplication.MobilityStatus.IN_REVIEW)); }
        @Test void submitted_to_rejected()  { assertDoesNotThrow(() -> engine.assertMobilityTransition(MobilityApplication.MobilityStatus.SUBMITTED, MobilityApplication.MobilityStatus.REJECTED)); }
        @Test void in_review_to_approved()  { assertDoesNotThrow(() -> engine.assertMobilityTransition(MobilityApplication.MobilityStatus.IN_REVIEW, MobilityApplication.MobilityStatus.APPROVED)); }
        @Test void in_review_to_rejected()  { assertDoesNotThrow(() -> engine.assertMobilityTransition(MobilityApplication.MobilityStatus.IN_REVIEW, MobilityApplication.MobilityStatus.REJECTED)); }

        @Test
        @DisplayName("APPROVED and REJECTED are terminal states")
        void terminal_states_have_no_transitions() {
            assertThat(engine.allowedMobilityTransitions(MobilityApplication.MobilityStatus.APPROVED)).isEmpty();
            assertThat(engine.allowedMobilityTransitions(MobilityApplication.MobilityStatus.REJECTED)).isEmpty();
        }

        @Test
        @DisplayName("DRAFT → APPROVED skips review process and is invalid")
        void draft_to_approved_invalid() {
            assertThatThrownBy(() -> engine.assertMobilityTransition(MobilityApplication.MobilityStatus.DRAFT, MobilityApplication.MobilityStatus.APPROVED))
                    .isInstanceOf(IllegalStateException.class);
        }
    }

    // =========================================================================
    // GradeChange transitions
    // =========================================================================

    @Nested
    @DisplayName("Grade change request status transitions")
    class GradeChangeTransitions {

        @Test void submitted_to_approved()  { assertDoesNotThrow(() -> engine.assertGradeChangeTransition(GradeChangeRequest.RequestStatus.SUBMITTED, GradeChangeRequest.RequestStatus.APPROVED)); }
        @Test void submitted_to_rejected()  { assertDoesNotThrow(() -> engine.assertGradeChangeTransition(GradeChangeRequest.RequestStatus.SUBMITTED, GradeChangeRequest.RequestStatus.REJECTED)); }
        @Test void approved_to_applied()    { assertDoesNotThrow(() -> engine.assertGradeChangeTransition(GradeChangeRequest.RequestStatus.APPROVED, GradeChangeRequest.RequestStatus.APPLIED)); }

        @Test
        @DisplayName("REJECTED and APPLIED are terminal states")
        void terminal_states_are_empty() {
            assertThat(engine.allowedGradeChangeTransitions(GradeChangeRequest.RequestStatus.REJECTED)).isEmpty();
            assertThat(engine.allowedGradeChangeTransitions(GradeChangeRequest.RequestStatus.APPLIED)).isEmpty();
        }

        @Test
        @DisplayName("SUBMITTED → APPLIED skips approval step and is invalid")
        void submitted_to_applied_invalid() {
            assertThatThrownBy(() -> engine.assertGradeChangeTransition(GradeChangeRequest.RequestStatus.SUBMITTED, GradeChangeRequest.RequestStatus.APPLIED))
                    .isInstanceOf(IllegalStateException.class);
        }
    }

    // =========================================================================
    // Student request transitions
    // =========================================================================

    @Nested
    @DisplayName("Student request status transitions")
    class RequestTransitions {

        @Test void new_to_in_review()     { assertDoesNotThrow(() -> engine.assertRequestTransition(StudentRequest.RequestStatus.NEW, StudentRequest.RequestStatus.IN_REVIEW)); }
        @Test void new_to_need_info()     { assertDoesNotThrow(() -> engine.assertRequestTransition(StudentRequest.RequestStatus.NEW, StudentRequest.RequestStatus.NEED_INFO)); }
        @Test void new_to_approved()      { assertDoesNotThrow(() -> engine.assertRequestTransition(StudentRequest.RequestStatus.NEW, StudentRequest.RequestStatus.APPROVED)); }
        @Test void new_to_rejected()      { assertDoesNotThrow(() -> engine.assertRequestTransition(StudentRequest.RequestStatus.NEW, StudentRequest.RequestStatus.REJECTED)); }
        @Test void in_review_to_done()    { assertDoesNotThrow(() -> engine.assertRequestTransition(StudentRequest.RequestStatus.IN_REVIEW, StudentRequest.RequestStatus.DONE)); }
        @Test void approved_to_done()     { assertDoesNotThrow(() -> engine.assertRequestTransition(StudentRequest.RequestStatus.APPROVED, StudentRequest.RequestStatus.DONE)); }
        @Test void rejected_to_done()     { assertDoesNotThrow(() -> engine.assertRequestTransition(StudentRequest.RequestStatus.REJECTED, StudentRequest.RequestStatus.DONE)); }
        @Test void need_info_to_in_review(){ assertDoesNotThrow(() -> engine.assertRequestTransition(StudentRequest.RequestStatus.NEED_INFO, StudentRequest.RequestStatus.IN_REVIEW)); }

        @Test
        @DisplayName("DONE is a terminal state — no further transitions allowed")
        void done_is_terminal() {
            assertThat(engine.allowedRequestTransitions(StudentRequest.RequestStatus.DONE)).isEmpty();
        }

        @Test
        @DisplayName("NEW → DONE is invalid (must resolve before archiving)")
        void new_to_done_invalid() {
            assertThatThrownBy(() -> engine.assertRequestTransition(StudentRequest.RequestStatus.NEW, StudentRequest.RequestStatus.DONE))
                    .isInstanceOf(IllegalStateException.class);
        }
    }

    // =========================================================================
    // Clearance checkpoint transitions
    // =========================================================================

    @Nested
    @DisplayName("Clearance checkpoint status transitions")
    class ClearanceCheckpointTransitions {

        @Test void pending_to_approved()  { assertDoesNotThrow(() -> engine.assertClearanceCheckpointTransition(ClearanceCheckpoint.CheckpointStatus.PENDING, ClearanceCheckpoint.CheckpointStatus.APPROVED)); }
        @Test void pending_to_rejected()  { assertDoesNotThrow(() -> engine.assertClearanceCheckpointTransition(ClearanceCheckpoint.CheckpointStatus.PENDING, ClearanceCheckpoint.CheckpointStatus.REJECTED)); }
        @Test void rejected_to_approved() { assertDoesNotThrow(() -> engine.assertClearanceCheckpointTransition(ClearanceCheckpoint.CheckpointStatus.REJECTED, ClearanceCheckpoint.CheckpointStatus.APPROVED)); }

        @Test
        @DisplayName("APPROVED is a terminal state — cannot be reversed")
        void approved_is_terminal() {
            assertThat(engine.allowedClearanceCheckpointTransitions(ClearanceCheckpoint.CheckpointStatus.APPROVED)).isEmpty();
        }

        @Test
        @DisplayName("APPROVED → REJECTED is invalid (cannot un-approve)")
        void approved_to_rejected_invalid() {
            assertThatThrownBy(() -> engine.assertClearanceCheckpointTransition(
                    ClearanceCheckpoint.CheckpointStatus.APPROVED, ClearanceCheckpoint.CheckpointStatus.REJECTED))
                    .isInstanceOf(IllegalStateException.class);
        }
    }
}
