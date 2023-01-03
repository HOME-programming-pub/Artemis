package de.tum.in.www1.artemis.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.hibernate.Hibernate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Service;

import de.tum.in.www1.artemis.domain.Result;
import de.tum.in.www1.artemis.domain.participation.Participation;
import de.tum.in.www1.artemis.domain.quiz.*;
import de.tum.in.www1.artemis.repository.*;

@Service
public class QuizStatisticService {

    private final Logger log = LoggerFactory.getLogger(QuizStatisticService.class);

    private final StudentParticipationRepository studentParticipationRepository;

    private final ResultRepository resultRepository;

    private final QuizPointStatisticRepository quizPointStatisticRepository;

    private final QuizQuestionStatisticRepository quizQuestionStatisticRepository;

    private final QuizSubmissionRepository quizSubmissionRepository;

    private final SimpMessageSendingOperations messagingTemplate;

    private final MultipleChoiceQuestionStatisticRepository multipleChoiceQuestionStatisticRepository;

    private final DragAndDropQuestionStatisticRepository dragAndDropQuestionStatisticRepository;

    private final ShortAnswerQuestionStatisticRepository shortAnswerQuestionStatisticRepository;

    public QuizStatisticService(StudentParticipationRepository studentParticipationRepository, ResultRepository resultRepository, SimpMessageSendingOperations messagingTemplate,
            QuizPointStatisticRepository quizPointStatisticRepository, QuizQuestionStatisticRepository quizQuestionStatisticRepository,
            QuizSubmissionRepository quizSubmissionRepository, MultipleChoiceQuestionStatisticRepository multipleChoiceQuestionStatisticRepository,
            DragAndDropQuestionStatisticRepository dragAndDropQuestionStatisticRepository, ShortAnswerQuestionStatisticRepository shortAnswerQuestionStatisticRepository) {
        this.studentParticipationRepository = studentParticipationRepository;
        this.resultRepository = resultRepository;
        this.quizPointStatisticRepository = quizPointStatisticRepository;
        this.quizQuestionStatisticRepository = quizQuestionStatisticRepository;
        this.messagingTemplate = messagingTemplate;
        this.quizSubmissionRepository = quizSubmissionRepository;
        this.multipleChoiceQuestionStatisticRepository = multipleChoiceQuestionStatisticRepository;
        this.dragAndDropQuestionStatisticRepository = dragAndDropQuestionStatisticRepository;
        this.shortAnswerQuestionStatisticRepository = shortAnswerQuestionStatisticRepository;
    }

    /**
     * 1. Go through all Results in the Participation and recalculate the score
     * 2. Recalculate the statistics of the given quizExercise
     *
     * @param quizExercise the changed QuizExercise object which will be used to recalculate the existing Results and Statistics
     */
    public void recalculateStatistics(QuizExercise quizExercise) {

        // reset all statistics
        if (quizExercise.getQuizPointStatistic() != null) {
            quizExercise.getQuizPointStatistic().resetStatistic();
        }
        else {
            var quizPointStatistic = new QuizPointStatistic();
            quizExercise.setQuizPointStatistic(quizPointStatistic);
            quizPointStatistic.setQuiz(quizExercise);
            quizExercise.recalculatePointCounters();
        }
        for (QuizQuestion quizQuestion : quizExercise.getQuizQuestions()) {
            if (quizQuestion.getQuizQuestionStatistic() != null) {
                if (quizQuestion instanceof MultipleChoiceQuestion mcQuestion) {
                    var quizQuestionStatistic = (MultipleChoiceQuestionStatistic) mcQuestion.getQuizQuestionStatistic();
                    if (!Hibernate.isInitialized(quizQuestionStatistic) || !Hibernate.isInitialized(quizQuestionStatistic.getAnswerCounters())) {
                        // Note: load the quizQuestionStatistic from database with answerCounters
                        quizQuestionStatistic = multipleChoiceQuestionStatisticRepository.findByIdWithAnswerCounters(quizQuestionStatistic.getId());
                        mcQuestion.setQuizQuestionStatistic(quizQuestionStatistic);
                    }
                }
                else if (quizQuestion instanceof DragAndDropQuestion dndQuestion) {
                    var quizQuestionStatistic = (DragAndDropQuestionStatistic) dndQuestion.getQuizQuestionStatistic();
                    if (!Hibernate.isInitialized(quizQuestionStatistic) || !Hibernate.isInitialized(quizQuestionStatistic.getDropLocationCounters())) {
                        // Note: load the quizQuestionStatistic from database with dropLocationCounters
                        quizQuestionStatistic = dragAndDropQuestionStatisticRepository.findByIdWithDropLocationCounters(quizQuestionStatistic.getId());
                        dndQuestion.setQuizQuestionStatistic(quizQuestionStatistic);
                    }
                }
                else if (quizQuestion instanceof ShortAnswerQuestion saQuestion) {
                    var quizQuestionStatistic = (ShortAnswerQuestionStatistic) saQuestion.getQuizQuestionStatistic();
                    if (!Hibernate.isInitialized(quizQuestionStatistic) || !Hibernate.isInitialized(quizQuestionStatistic.getShortAnswerSpotCounters())) {
                        // Note: load the quizQuestionStatistic from database with shortAnswerSpotCounters
                        quizQuestionStatistic = shortAnswerQuestionStatisticRepository.findByIdWithShortAnswerSpotCounters(quizQuestionStatistic.getId());
                        saQuestion.setQuizQuestionStatistic(quizQuestionStatistic);
                    }
                }
                quizQuestion.getQuizQuestionStatistic().resetStatistic();
            }
        }

        // add the Results in every participation of the given quizExercise to the statistics
        for (Participation participation : studentParticipationRepository.findByExerciseId(quizExercise.getId())) {

            Result latestRatedResult = null;
            Result latestUnratedResult = null;

            var results = resultRepository.findAllByParticipationIdOrderByCompletionDateDesc(participation.getId());
            // update all Results of a participation
            for (Result result : results) {

                // find the latest rated Result
                if (Boolean.TRUE.equals(result.isRated()) && (latestRatedResult == null || latestRatedResult.getCompletionDate().isBefore(result.getCompletionDate()))) {
                    latestRatedResult = result;
                }
                // find latest unrated Result
                if (Boolean.FALSE.equals(result.isRated()) && (latestUnratedResult == null || latestUnratedResult.getCompletionDate().isBefore(result.getCompletionDate()))) {
                    latestUnratedResult = result;
                }
            }
            // update statistics with the latest rated und unrated Result
            if (latestRatedResult != null && latestRatedResult.getSubmission() != null) {
                var latestRatedSubmission = quizSubmissionRepository.findWithEagerSubmittedAnswersById(latestRatedResult.getSubmission().getId());
                quizExercise.addResultToAllStatistics(latestRatedResult, latestRatedSubmission);
            }
            if (latestUnratedResult != null && latestUnratedResult.getSubmission() != null) {
                var latestUnratedSubmission = quizSubmissionRepository.findWithEagerSubmittedAnswersById(latestUnratedResult.getSubmission().getId());
                quizExercise.addResultToAllStatistics(latestUnratedResult, latestUnratedSubmission);
            }
        }

        // save changed Statistics
        quizPointStatisticRepository.saveAndFlush(quizExercise.getQuizPointStatistic());
        for (QuizQuestion quizQuestion : quizExercise.getQuizQuestions()) {
            if (quizQuestion.getQuizQuestionStatistic() != null) {
                quizQuestionStatisticRepository.saveAndFlush(quizQuestion.getQuizQuestionStatistic());
            }
        }
    }

    /**
     * 1. check for each result if it's rated -> true: check if there is an old Result -> true: remove the old Result from the statistics 2. add new Result to the
     * quiz-point-statistic and all question-statistics
     *
     * @param results the results, which will be added to the statistics
     * @param quiz    the quizExercise with Questions where the results should contain to
     */
    public void updateStatistics(Collection<Result> results, QuizExercise quiz) {

        if (results != null && quiz != null && quiz.getQuizQuestions() != null) {
            log.debug("update statistics with {} new results", results.size());

            log.debug("Load quiz point statistic with point counters");
            quiz.setQuizPointStatistic(quizPointStatisticRepository.findByIdWithPointCounters(quiz.getQuizPointStatistic().getId()));
            for (Result result : results) {
                // check if the result is rated
                // NOTE: there is never an old Result if the new result is rated
                if (Boolean.FALSE.equals(result.isRated())) {
                    quiz.removeResultFromAllStatistics(getPreviousResult(result));
                }
                log.debug("Load quiz submissions with submitted answers");
                var quizSubmission = quizSubmissionRepository.findWithEagerSubmittedAnswersById(result.getSubmission().getId());
                quiz.addResultToAllStatistics(result, quizSubmission);
            }
            // save statistics
            quizPointStatisticRepository.save(quiz.getQuizPointStatistic());
            List<QuizQuestionStatistic> quizQuestionStatistics = new ArrayList<>();
            for (QuizQuestion quizQuestion : quiz.getQuizQuestions()) {
                if (quizQuestion.getQuizQuestionStatistic() != null) {
                    quizQuestionStatistics.add(quizQuestion.getQuizQuestionStatistic());
                }
            }
            quizQuestionStatisticRepository.saveAll(quizQuestionStatistics);
            // notify users via websocket about new results for the statistics.
            // filters out solution information
            quiz.filterForStatisticWebsocket();
            messagingTemplate.convertAndSend("/topic/statistic/" + quiz.getId(), quiz);
        }
    }

    /**
     * Go through all Results in the Participation and return the latest one before the new Result,
     *
     * @param newResult the new result object which will replace the old Result in the Statistics
     * @return the previous Result, which is presented in the Statistics (null if where is no previous Result)
     */
    private Result getPreviousResult(Result newResult) {
        Result oldResult = null;

        List<Result> allResultsForParticipation = resultRepository.findAllByParticipationIdOrderByCompletionDateDesc(newResult.getParticipation().getId());
        for (Result result : allResultsForParticipation) {
            // find the latest Result, which is presented in the Statistics
            if (result.isRated() == newResult.isRated() && result.getCompletionDate().isBefore(newResult.getCompletionDate()) && !result.equals(newResult)
                    && (oldResult == null || result.getCompletionDate().isAfter(oldResult.getCompletionDate()))) {
                oldResult = result;
            }
        }
        return oldResult;
    }

    /**
     * Load quiz question statistics for each question together with their lazy relationships to avoid database issues (proxy issue or database constraint issue)
     * @param quizExercise the quiz exercise for which the question statistic details should be loaded
     */
    public void loadQuestionStatisticDetails(QuizExercise quizExercise) {
        for (var quizQuestion : quizExercise.getQuizQuestions()) {
            QuizQuestionStatistic statistic = null;
            if (quizQuestion instanceof MultipleChoiceQuestion mcQuestion && mcQuestion.getQuizQuestionStatistic() != null) {
                // Note: load the quizQuestionStatistic from database with answerCounters
                statistic = multipleChoiceQuestionStatisticRepository.findByIdWithAnswerCounters(mcQuestion.getQuizQuestionStatistic().getId());
                quizQuestion.setQuizQuestionStatistic(statistic);
            }
            else if (quizQuestion instanceof DragAndDropQuestion dndQuestion && dndQuestion.getQuizQuestionStatistic() != null) {
                // Note: load the quizQuestionStatistic from database with dropLocationCounters
                statistic = dragAndDropQuestionStatisticRepository.findByIdWithDropLocationCounters(dndQuestion.getQuizQuestionStatistic().getId());
                quizQuestion.setQuizQuestionStatistic(statistic);
            }
            else if (quizQuestion instanceof ShortAnswerQuestion saQuestion && saQuestion.getQuizQuestionStatistic() != null) {
                // Note: load the quizQuestionStatistic from database with shortAnswerSpotCounters
                statistic = shortAnswerQuestionStatisticRepository.findByIdWithShortAnswerSpotCounters(saQuestion.getQuizQuestionStatistic().getId());
                quizQuestion.setQuizQuestionStatistic(statistic);
            }
        }
    }
}
