
package com.enterprise.knowledge.dto;

import com.enterprise.knowledge.entity.AnswerRecord.Status;
import java.util.List;

public class AskResponse {

    private Long answerRecordId;
    private String question;
    private String answer;
    private Status status;
    private List<CitationDto> citations;
    private int evidenceChunksUsed;

    public AskResponse() {
    }

    public AskResponse(
            Long answerRecordId,
            String question,
            String answer,
            Status status,
            List<CitationDto> citations,
            int evidenceChunksUsed) {
        this.answerRecordId = answerRecordId;
        this.question = question;
        this.answer = answer;
        this.status = status;
        this.citations = citations;
        this.evidenceChunksUsed = evidenceChunksUsed;
    }

    public Long getAnswerRecordId() { return answerRecordId; }
    public void setAnswerRecordId(Long answerRecordId) { this.answerRecordId = answerRecordId; }

    public String getQuestion() { return question; }
    public void setQuestion(String question) { this.question = question; }

    public String getAnswer() { return answer; }
    public void setAnswer(String answer) { this.answer = answer; }

    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }

    public List<CitationDto> getCitations() { return citations; }
    public void setCitations(List<CitationDto> citations) { this.citations = citations; }

    public int getEvidenceChunksUsed() { return evidenceChunksUsed; }
    public void setEvidenceChunksUsed(int evidenceChunksUsed) {
        this.evidenceChunksUsed = evidenceChunksUsed;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long answerRecordId;
        private String question;
        private String answer;
        private Status status;
        private List<CitationDto> citations;
        private int evidenceChunksUsed;

        public Builder answerRecordId(Long answerRecordId) {
            this.answerRecordId = answerRecordId;
            return this;
        }

        public Builder question(String question) {
            this.question = question;
            return this;
        }

        public Builder answer(String answer) {
            this.answer = answer;
            return this;
        }

        public Builder status(Status status) {
            this.status = status;
            return this;
        }

        public Builder citations(List<CitationDto> citations) {
            this.citations = citations;
            return this;
        }

        public Builder evidenceChunksUsed(int evidenceChunksUsed) {
            this.evidenceChunksUsed = evidenceChunksUsed;
            return this;
        }

        public AskResponse build() {
            return new AskResponse(
                    answerRecordId,
                    question,
                    answer,
                    status,
                    citations,
                    evidenceChunksUsed);
        }
    }
}
