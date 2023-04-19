package paxos;

public enum MessageType {
    ACCEPT_OK,
    ACCEPT_REJECT,
    PREPARE_OK,
    PREPARE_REJECT,
    DECIDE_OK,
    DECIDE_REJECT
}