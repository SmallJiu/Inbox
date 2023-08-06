package cat.jiu.email.element;

public enum EmailSenderGroup {
    SYSTEM, PLAYER;
    public static EmailSenderGroup getGroupByID(int id) {
        switch(id) {
            case 1: return PLAYER;
            default: return SYSTEM;
        }
    }
    public static int getIDByGroup(EmailSenderGroup sender) {
        switch(sender) {
            case PLAYER: return 1;
            default: return 0;
        }
    }
    public boolean isPlayerSend() {
        return this == PLAYER;
    }
    public boolean isSystemSend() {
        return this == SYSTEM;
    }
}
