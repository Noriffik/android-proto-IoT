package io.relayr.iotsmartphone.handler;

import io.relayr.iotsmartphone.storage.Constants;
import io.relayr.iotsmartphone.storage.Storage;
import io.relayr.java.model.rules.IoTSmartphoneRule;

public class RuleBuilder extends IoTSmartphoneRule {

    private transient InputReading[] readings = new InputReading[2];
    private transient OutputCommand[] commands = new OutputCommand[2];

    public RuleBuilder() {}

    public void setCondition(int position, String deviceId, String meaning, String operation, int value) {
        readings[position] = new InputReading(deviceId, meaning);
        if (position == 0) {
            comparator1 = operation;
            constant1 = value;
        } else if (position == 1) {
            comparator2 = operation;
            constant2 = value;
        }
    }

    public void setCommand(int position, String deviceId, String name, boolean value) {
        commands[position] = new OutputCommand(deviceId, name, value);
    }

    public void setConditionOperator(String operator) {
        if (readings[0] == null || readings[1] == null) return;
        switch (operator) {
            case "&":
                operator = "and";
                break;
            case "||":
                operator = "or";
                break;
            default:
                operator = null;
                break;
        }
    }

    public String getConditionOperator() {
        if (operator == null) return null;
        else if (operator.equals("and")) return "&";
        else return "||";
    }

    public void removeCondition(int position) {
        if (position == 0) {
            constant1 = null;
            comparator1 = null;
        } else if (position == 1) {
            constant2 = null;
            comparator2 = null;
        }
        readings[position] = null;
    }

    public void removeOutcome(int position) {
        commands[position] = null;
    }

    public RuleBuilder build() {
        if (!hasCondition() || !hasOutcome()) return null;
        inputReadings.clear();
        outputCommands.clear();
        if (readings[0] != null) inputReadings.add(readings[0]);
        if (readings[1] != null) inputReadings.add(readings[1]);
        if (commands[0] != null) outputCommands.add(commands[0]);
        if (commands[1] != null) outputCommands.add(commands[1]);

        return this;
    }

    public String getConditionMeaning(int position) {
        if (inputReadings.size() < position + 1) return null;
        if (inputReadings.get(position) == null) return null;
        else return inputReadings.get(position).getMeaning();
    }

    public Constants.DeviceType getConditionType(int position) {
        if (inputReadings.size() < position + 1) return null;
        if (inputReadings.get(position) == null) return null;
        else return Storage.instance().getDeviceType(inputReadings.get(position).getDeviceId());
    }

    public String getConditionOperator(int position) {
        if (position == 0 && comparator1 != null) return (String) comparator1;
        else if (position == 1 && comparator2 != null) return (String) comparator2;
        else return null;
    }

    public int getConditionValue(int position) {
        if (position == 0 && constant1 != null) return ((Number) constant1).intValue();
        else if (position == 1 && constant2 != null) return ((Number) constant2).intValue();
        else return 0;
    }

    public String getOutcomeName(int position) {
        if (outputCommands.size() < position + 1) return null;
        if (outputCommands.get(position) == null) return null;
        else return outputCommands.get(position).getName();
    }

    public Boolean getOutcomeValue(int position) {
        if (outputCommands.size() < position + 1) return null;
        if (outputCommands.get(position) == null) return null;
        else return (Boolean) outputCommands.get(position).getValue();
    }

    public boolean hasCondition() {
        for (InputReading reading : readings) if (reading != null) return true;
        return false;
    }

    public boolean hasOutcome() {
        for (OutputCommand cmd : commands) if (cmd != null) return true;
        return false;
    }

    public void initialize() {
        for (int i = 0; i < inputReadings.size(); i++) readings[i] = inputReadings.get(i);
        for (int i = 0; i < outputCommands.size(); i++) commands[i] = outputCommands.get(i);
    }

}