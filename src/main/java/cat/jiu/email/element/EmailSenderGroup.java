package cat.jiu.email.element;

public enum EmailSenderGroup {
    SYSTEM, PLAYER;
    public static EmailSenderGroup getGroupByID(int id) {
        return id == 1 ? PLAYER : SYSTEM;
    }
    public static int getIDByGroup(EmailSenderGroup sender) {
        return sender == EmailSenderGroup.PLAYER ? 1 :0;
    }
    public boolean isPlayerSend() {
        return this == PLAYER;
    }
    public boolean isSystemSend() {
        return this == SYSTEM;
    }
}
