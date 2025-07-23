package cat.jiu.email.element;

import java.util.Objects;

public enum EmailSenderGroup {
    SYSTEM, PLAYER;
    public static EmailSenderGroup getGroupByID(int id) {
        if (id == 1) {
            return PLAYER;
        }
        return SYSTEM;
    }
    public static int getIDByGroup(EmailSenderGroup sender) {
        if (Objects.requireNonNull(sender) == EmailSenderGroup.PLAYER) {
            return 1;
        }
        return 0;
    }
    public boolean isPlayerSend() {
        return this == PLAYER;
    }
    public boolean isSystemSend() {
        return this == SYSTEM;
    }
}
