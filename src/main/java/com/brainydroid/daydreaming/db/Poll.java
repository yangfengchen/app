package com.brainydroid.daydreaming.db;

import com.brainydroid.daydreaming.background.Logger;
import com.google.gson.annotations.Expose;
import com.google.inject.Inject;

import java.util.ArrayList;

public final class Poll extends StatusModel<Poll,PollsStorage> {

    private static String TAG = "Poll";

    @Expose @Inject private ArrayList<Question> questions;
    @Expose private long notificationNtpTimestamp;
    @Expose private long notificationSystemTimestamp;

    public static final String STATUS_PENDING = "pollPending"; // Notification has appeared
    public static final String STATUS_RUNNING = "pollRunning"; // QuestionActivity is running
    public static final String STATUS_PARTIALLY_COMPLETED = "pollPartiallyCompleted"; // QuestionActivity was stopped, and Poll expired
    public static final String STATUS_COMPLETED = "pollCompleted"; // QuestionActivity completed

    @Inject transient PollsStorage pollsStorage;
    @Inject transient QuestionsStorage questionsStorage;

    public synchronized void populateQuestions(int nQuestions) {
        Logger.d(TAG, "Populating poll with {0} questions", nQuestions);
        questions = questionsStorage.getRandomQuestions(nQuestions);
        for (Question question : questions) {
            question.setPoll(this);
        }
    }

    public synchronized void addQuestion(Question question) {
        Logger.d(TAG, "Adding question {0} to poll", question.getName());
        question.setPoll(this);
        questions.add(question);
    }

    public synchronized ArrayList<Question> getQuestions() {
        return questions;
    }

    public synchronized Question getQuestionByIndex(int index) {
        return questions.get(index);
    }

    public synchronized int getLength() {
        return questions.size();
    }

    public synchronized long getNtpNotificationTimestamp() {
        return notificationNtpTimestamp;
    }

    public synchronized void setNotificationNtpTimestamp(
            long notificationNtpTimestamp) {
        Logger.v(TAG, "Setting notification ntpTimestamp");
        this.notificationNtpTimestamp = notificationNtpTimestamp;
        saveIfSync();
    }

    public synchronized long getSystemNotificationTimestamp() {
        return notificationSystemTimestamp;
    }

    public synchronized void setNotificationSystemTimestamp(
            long notificationSystemTimestamp) {
        Logger.v(TAG, "Setting notification systemTimestamp");
        this.notificationSystemTimestamp = notificationSystemTimestamp;
        saveIfSync();
    }

    @Override
    protected synchronized Poll self() {
        return this;
    }

    @Override
    protected synchronized PollsStorage getStorage() {
        return pollsStorage;
    }

}
