package walknroll.zoodini.models.entities;

import com.badlogic.gdx.maps.MapProperties;

import walknroll.zoodini.utils.enums.AvatarType;

public abstract class PlayableAvatar extends Avatar {
    public abstract float getAbilityRange();

    public abstract boolean canUseAbility();

    public abstract void setCurrentlyAiming(boolean value);

    public abstract boolean isCurrentlyAiming();

    public abstract boolean didFire();

    public abstract void setDidFire(boolean value);

    public PlayableAvatar(AvatarType avatarType, MapProperties properties, float units) {
        super(avatarType, properties, units);
    }
}
