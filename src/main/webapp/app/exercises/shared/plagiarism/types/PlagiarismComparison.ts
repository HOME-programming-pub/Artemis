import { PlagiarismResult } from 'app/exercises/shared/plagiarism/types/PlagiarismResult';
import { PlagiarismSubmissionElement } from 'app/exercises/shared/plagiarism/types/PlagiarismSubmissionElement';
import { PlagiarismMatch } from './PlagiarismMatch';
import { PlagiarismStatus } from './PlagiarismStatus';
import { PlagiarismSubmission } from './PlagiarismSubmission';

/**
 * Pair of compared student submissions whose similarity is above a certain threshold.
 */
export class PlagiarismComparison<E extends PlagiarismSubmissionElement> {
    /**
     * Unique identifier of the comparison.
     */
    id: number;

    /**
     * The plagiarism result
     */
    plagiarismResult?: PlagiarismResult<PlagiarismSubmissionElement>;

    /**
     * First submission involved in this comparison.
     */
    submissionA: PlagiarismSubmission<E>;

    /**
     * Second submission involved in this comparison.
     */
    submissionB: PlagiarismSubmission<E>;

    /**
     * List of matches between both submissions involved in this comparison.
     */
    matches?: PlagiarismMatch[];

    /**
     * Similarity of the compared submissions in percentage (between 0 and 100).
     */
    similarity: number;

    /**
     * Status of this submission comparison.
     */
    status: PlagiarismStatus;
}
