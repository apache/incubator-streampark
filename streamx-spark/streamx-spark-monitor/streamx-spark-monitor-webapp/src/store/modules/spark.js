export default {
    namespaced: true,
    state: {
        confType: null,
        recordId: null,
        myId: null,
    },
    mutations: {
        setConfType(state, confType) {
            state.confType = confType
        },
        setRecordId(state, recordId) {
            state.recordId = recordId
        },
        setMyId(state, myId) {
            state.myId = myId
        }
    }
}