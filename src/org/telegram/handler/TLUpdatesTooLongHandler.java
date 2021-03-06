package org.telegram.handler;

import org.telegram.ApiState;
import org.telegram.ApiStorage;
import org.telegram.api.TLAbsMessage;
import org.telegram.api.TLAbsUpdates;
import org.telegram.api.TLUpdateNewMessage;
import org.telegram.api.TLUpdatesTooLong;
import org.telegram.api.engine.RpcCallbackEx;
import org.telegram.api.engine.TelegramApi;
import org.telegram.api.requests.TLRequestUpdatesGetDifference;
import org.telegram.api.updates.TLAbsDifference;
import org.telegram.api.updates.TLDifference;
import org.telegram.api.updates.TLDifferenceSlice;
import org.telegram.api.updates.TLState;
import org.telegram.tl.TLVector;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Created by s_tayari on 12/24/2017.
 */
public class TLUpdatesTooLongHandler implements TLAbsUpdatesHandler {

    private static final Logger LOGGER = Logger.getLogger(TLUpdatesTooLongHandler.class.getSimpleName());

    private List<TLAbsUpdateHandler> updateHandlers = new ArrayList<>();
    private TelegramApi api;

    public TLUpdatesTooLongHandler(TelegramApi api) {
        this.api = api;
        updateHandlers.add(new TLUpdateNewMessageHandler(api));
    }

    @Override
    public boolean canProcess(int updateClassId) {
        return TLUpdatesTooLong.CLASS_ID == updateClassId;
    }

    @Override
    public void processUpdates(TLAbsUpdates updates) {
        ApiStorage apiStorage = (ApiStorage) api.getState();
        ApiState apiState = new ApiState(apiStorage.getObj().getPhone().replaceAll("\\+", ""));
        TLState tlState = apiState.getObj();
        api.doRpcCall(new TLRequestUpdatesGetDifference(tlState.getPts(), tlState.getDate(), tlState.getQts()), new RpcCallbackEx<TLAbsDifference>() {

            @Override
            public void onConfirmed() {

            }

            @Override
            public void onResult(TLAbsDifference result) {
                TLState newState = null;
                if(result instanceof TLDifferenceSlice) {
                    TLVector<TLAbsMessage> newMessages = ((TLDifferenceSlice) result).getNewMessages();
                    processNewMessages(newMessages);
                    newState = ((TLDifferenceSlice) result).getIntermediateState();
                } else if(result instanceof TLDifference) {
                    TLVector<TLAbsMessage> newMessages = ((TLDifference) result).getNewMessages();
                    processNewMessages(newMessages);
                    newState = ((TLDifference) result).getState();
                }

                if(newState != null)
                    apiState.updateState(newState);
            }

            @Override
            public void onError(int errorCode, String message) {

            }
        });

    }

    private void processNewMessages(TLVector<TLAbsMessage> newMessages) {
        for(TLAbsMessage newMessage : newMessages) {
            TLUpdateNewMessage tlUpdateNewMessage = new TLUpdateNewMessage(newMessage, 0);
            for (TLAbsUpdateHandler updateHandler : updateHandlers) {
                if(updateHandler.canProcess(tlUpdateNewMessage.getClassId()))
                    updateHandler.processUpdate(tlUpdateNewMessage);
            }
        }
    }
}
