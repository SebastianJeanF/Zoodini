package walknroll.zoodini.models.entities;

import com.badlogic.gdx.maps.MapProperties;
import com.badlogic.gdx.utils.JsonValue;

import walknroll.zoodini.utils.Constants;
import walknroll.zoodini.utils.enums.AvatarType;

public abstract class PlayableAvatar extends Avatar {
    private boolean invincible;

    private int numKeys;

    public PlayableAvatar(AvatarType avatarType, MapProperties properties, JsonValue constants, float units) {
        super(avatarType, properties, constants, units);
        numKeys = 0;
        invincible = false;
    }

    public boolean isInvincible() {
        return Constants.INVINCIBLE || invincible;
    }

    public void setInvincible(boolean invincible) {
        this.invincible = invincible;
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

    public int getNumKeys() {
        return numKeys;
    }

    public void decreaseNumKeys() {
        numKeys--;
    }

    public void increaseNumKeys() {
        numKeys++;
    }

}
