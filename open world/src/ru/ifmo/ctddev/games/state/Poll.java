package ru.ifmo.ctddev.games.state;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Created by Aksenov239 on 30.08.2014.
 */
public class Poll {
    private int id;
    private String question;
    private String[] optionsName;

    @JsonIgnore
    private int[] optionsId;

    private int[] minimalAmount;
    private int priority = 0;
    private Integer result;
    private String date;
    private int[] investedMoney;

    public Poll() {
    }

    public Poll(int id, String question, int[] optionsId, String[] optionsName, int[] minimalAmount, int[] investedMoney) {
        this.id = id;
        this.question = question;
        this.optionsId = optionsId;
        this.optionsName = optionsName;
        this.minimalAmount = minimalAmount;
        this.investedMoney = investedMoney;
    }

    public void vote(int option, int amount) {
        for (int i = 0; i < optionsId.length; ++i)
            if (optionsId[i] == option)
                investedMoney[i] += amount;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public String[] getOptionsName() {
        return optionsName;
    }

    public void setOptionsName(String[] optionsName) {
        this.optionsName = optionsName;
    }

    public String getOptionName(int id) {
        for (int i = 0; i < optionsId.length; ++i)
            if (optionsId[i] == id)
                return optionsName[i];
        return null;
    }

    public int getMinimalAmountById(int id) {
        for (int i = 0; i < optionsName[i].length(); ++i)
            if (optionsId[i] == id)
                return minimalAmount[i];
        return -1;
    }

    public int getMinimalAmountByName(String name) {
        for (int i = 0; i < optionsName[i].length(); ++i)
            if (optionsName[i].equals(name))
                return minimalAmount[i];
        return -1;
    }

    public int getOptionId(String name) {
        for (int i = 0; i < optionsName[i].length(); ++i)
            if (optionsName[i].equals(name))
                return optionsId[i];
        return -1;
    }

    public void setMinimalAmount(int[] minimalAmount) {
        this.minimalAmount = minimalAmount;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public Integer getResult() {
        return result;
    }

    public void setResult(Integer result) {
        this.result = result;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public boolean containsOption(int id) {
        for (int i = 0; i < optionsId.length; ++i)
            if (optionsId[i] == id)
                return true;
        return false;
    }

    public boolean containsOption(String name) {
        for (int i = 0; i < optionsId.length; ++i)
            if (optionsName[i].equals(name))
                return true;
        return false;
    }


    @JsonIgnore
    public int[] getOptionsId() {
        return optionsId;
    }

    @JsonIgnore
    public void setOptionsId(int[] optionsId) {
        this.optionsId = optionsId;
    }

    public void setInvestedMoney(int[] investedMoney) {
        this.investedMoney = investedMoney;
    }

    public int[] getMinimalAmount() {
        return minimalAmount;
    }

    public int[] getInvestedMoney() {
        return investedMoney;
    }
}
