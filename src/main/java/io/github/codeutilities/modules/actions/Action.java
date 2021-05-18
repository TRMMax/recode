package io.github.codeutilities.modules.actions;

import io.github.codeutilities.modules.actions.impl.MessageAction;
import io.github.codeutilities.modules.actions.impl.StopIfEqualAction;
import io.github.codeutilities.modules.actions.impl.StopIfNotEqualAction;
import io.github.codeutilities.modules.actions.impl.WaitAction;
import io.github.codeutilities.modules.actions.json.ActionJson;

import java.util.HashMap;

public class Action {

    private static final Action[] ACTIONS = new Action[]{
            new MessageAction(),
            new StopIfEqualAction(),
            new StopIfNotEqualAction(),
            new WaitAction()
    };
    // actionId, action
    private static HashMap<String, Action> ACTION_IDS = new HashMap<>();

    public static void cacheActions() {
        for (Action action : ACTIONS) {
            ACTION_IDS.put(action.getId(), action);
        }
    }

    public String getId() {
        return null;
    }

    public void execute(ActionJson params) {}

    public static Action getAction(String id) {
        return ACTION_IDS.get(id);
    }

}
