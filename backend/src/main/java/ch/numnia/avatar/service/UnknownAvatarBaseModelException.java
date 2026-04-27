package ch.numnia.avatar.service;

/** UC-007 BR-004: avatar base model must come from the gender-neutral catalogue. */
public class UnknownAvatarBaseModelException extends RuntimeException {

    private final String baseModel;

    public UnknownAvatarBaseModelException(String baseModel) {
        super("unknown avatar base model: " + baseModel);
        this.baseModel = baseModel;
    }

    public String baseModel() { return baseModel; }
}
