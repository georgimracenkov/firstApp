package com.example.kursov.models;

import androidx.annotation.NonNull;

public class Answer {
    private String answer;
    private boolean isTrue;

    public Answer() {

    }

    public Answer(String answer, boolean isTrue) {
        this.answer = answer;
        this.isTrue = isTrue;
    }

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }

    public boolean isTrue() {
        return isTrue;
    }

    public void setTrue(boolean aTrue) {
        isTrue = aTrue;
    }

    @NonNull
    @Override
    public String toString() {
        return "Answer{" +
                "answer='" + answer + '\'' +
                ", isTrue=" + isTrue +
                '}';
    }
}
