package me.xap3y.space.model.pcv;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlaycoreStorageModel {

    private String version;
    private String uniqueId;

    private List<VipPackage> vipPackages = new ArrayList<>();
    private List<ActivePackage> activePackages = new ArrayList<>();
    private List<PausedPackage> pausedPackages = new ArrayList<>();
    private List<PlaycoreCode> codes = new ArrayList<>();
    private List<String> sessionIds = new ArrayList<>();

    public PlaycoreStorageModel(String version, String uniqueId) {
        this.version = version;
        this.uniqueId = uniqueId;
    }

    public void addOrReplaceVipPackage(VipPackage vipPackage) {
        vipPackages.removeIf(pkg -> pkg.getName().equals(vipPackage.getName()));
        vipPackages.add(vipPackage);
    }

    public void addOrReplaceActivePackage(ActivePackage activePackage) {
        activePackages.removeIf(pkg -> pkg.getPlayerUniqueId().equals(activePackage.getPlayerUniqueId())
                && pkg.getPackageName().equals(activePackage.getPackageName()));
        activePackages.add(activePackage);
    }

    public void addOrReplaceCode(PlaycoreCode code) {
        codes.removeIf(c -> c.getCode().equals(code.getCode()));
        codes.add(code);
    }

    public void addOrReplacePausedPackage(PausedPackage pausedPackage) {
        pausedPackages.removeIf(p -> p.getUuid().equals(pausedPackage.getUuid())
                && p.getPackageUi().equals(pausedPackage.getPackageUi()));
        pausedPackages.add(pausedPackage);
    }
}
