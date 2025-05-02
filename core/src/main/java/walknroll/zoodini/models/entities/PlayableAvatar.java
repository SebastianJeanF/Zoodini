package walknroll.zoodini.models.entities;

import com.badlogic.gdx.maps.MapProperties;

import com.badlogic.gdx.utils.JsonValue;
import walknroll.zoodini.utils.enums.AvatarType;

public abstract class PlayableAvatar extends Avatar {
    public PlayableAvatar(AvatarType avatarType, MapProperties properties, JsonValue constants, float units) {
        super(avatarType, properties, constants, units);
    }

    public abstract float getAbilityRange();

    public abstract boolean canUseAbility();

    public abstract void setCurrentlyAiming(boolean value);

    public abstract boolean isCurrentlyAiming();

    public abstract boolean didFire();

    public abstract void setDidFire(boolean value);

    @Override
    public float getForce() {
        return super.getForce() / (this.isCurrentlyAiming() ? 2f : 1f);
    }
}
