package ch.numnia.learning.api;

import ch.numnia.learning.domain.MathTask;
import ch.numnia.learning.domain.Operation;
import ch.numnia.learning.infra.InMemoryTaskPoolRepository;
import ch.numnia.learning.service.ExplanationSteps;
import ch.numnia.learning.service.SessionSummary;
import ch.numnia.learning.service.TrainingService;
import ch.numnia.learning.domain.AnswerResult;
import ch.numnia.learning.domain.TrainingSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * REST controller for the training mode (UC-003).
 *
 * <p>Auth note: child-session enforcement will move behind Spring Security
 * with UC-009. The {@code X-Child-Id} header is treated as authoritative for
 * now (matches UC-001/UC-002 placeholder approach).
 */
@RestController
@RequestMapping("/api/training")
public class TrainingController {

    private final TrainingService trainingService;

    public TrainingController(TrainingService trainingService) {
        this.trainingService = trainingService;
    }

    @PostMapping("/sessions")
    public ResponseEntity<Map<String, Object>> startSession(
            @RequestHeader("X-Child-Id") UUID childId,
            @RequestBody StartSessionRequest req) {
        TrainingSession session = trainingService.startSession(
                childId,
                req.operation(),
                req.worldId() != null ? req.worldId() : InMemoryTaskPoolRepository.DEFAULT_WORLD);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "sessionId", session.id(),
                "operation", session.operation(),
                "difficulty", session.currentDifficulty(),
                "speed", session.currentSpeed(),
                "accuracyMode", session.accuracyMode()));
    }

    /** UC-004: start a session in accuracy mode (G0, no time pressure). */
    @PostMapping("/accuracy-sessions")
    public ResponseEntity<Map<String, Object>> startAccuracySession(
            @RequestHeader("X-Child-Id") UUID childId,
            @RequestBody StartSessionRequest req) {
        TrainingSession session = trainingService.startAccuracySession(
                childId,
                req.operation(),
                req.worldId() != null ? req.worldId() : InMemoryTaskPoolRepository.DEFAULT_WORLD);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "sessionId", session.id(),
                "operation", session.operation(),
                "difficulty", session.currentDifficulty(),
                "speed", session.currentSpeed(),
                "accuracyMode", session.accuracyMode()));
    }

    @PostMapping("/sessions/{sessionId}/tasks")
    public ResponseEntity<Map<String, Object>> nextTask(@PathVariable UUID sessionId) {
        MathTask task = trainingService.nextTask(sessionId);
        return ResponseEntity.ok(Map.of(
                "taskId", task.id(),
                "operation", task.operation(),
                "operandA", task.operandA(),
                "operandB", task.operandB(),
                "difficulty", task.difficulty(),
                "speed", task.speed(),
                "timed", task.timed()));
    }

    /** UC-004 alt-flow 5a: animated solution steps for the current task. */
    @GetMapping("/sessions/{sessionId}/explanation")
    public ResponseEntity<ExplanationSteps> getExplanation(@PathVariable UUID sessionId) {
        return ResponseEntity.ok(trainingService.getExplanation(sessionId));
    }

    @PostMapping("/sessions/{sessionId}/answers")
    public ResponseEntity<AnswerResult> submitAnswer(@PathVariable UUID sessionId,
                                                     @RequestBody SubmitAnswerRequest req) {
        return ResponseEntity.ok(trainingService.submitAnswer(
                sessionId, req.answer(), req.responseTimeMs()));
    }

    @PostMapping("/sessions/{sessionId}/timeouts")
    public ResponseEntity<AnswerResult> submitTimeout(@PathVariable UUID sessionId) {
        return ResponseEntity.ok(trainingService.submitTimeout(sessionId));
    }

    @PostMapping("/sessions/{sessionId}/end")
    public ResponseEntity<SessionSummary> endSession(@PathVariable UUID sessionId) {
        return ResponseEntity.ok(trainingService.endSession(sessionId));
    }

    public record StartSessionRequest(Operation operation, String worldId) {}

    public record SubmitAnswerRequest(int answer, long responseTimeMs) {}
}
