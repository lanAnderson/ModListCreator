package org.moddingx.modlistcreator.changelogger;

import org.moddingx.modlistcreator.output.OutputTarget;
import org.moddingx.modlistcreator.platform.Modpack;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public class ChangelogFormatter {

    public static String format(Modpack from, Modpack to, OutputTarget.Type outputType) {
        Map<String, Modpack.File> oldInfoBySlug = from.files().stream().collect(Collectors.toMap(Modpack.File::projectSlug, info -> info));
        Map<String, Modpack.File> newInfoBySlug = to.files().stream().collect(Collectors.toMap(Modpack.File::projectSlug, info -> info));

        boolean changedLoader = !from.minecraft().loaderVersion().equals(to.minecraft().loaderVersion());
        List<Modpack.File> added = to.files().stream()
                .filter(file -> !oldInfoBySlug.containsKey(file.projectSlug()))
                .sorted(Comparator.comparing(o -> o.projectName().toLowerCase(Locale.ROOT)))
                .toList();
        List<Modpack.File> removed = from.files().stream()
                .filter(file -> !newInfoBySlug.containsKey(file.projectSlug()))
                .sorted(Comparator.comparing(o -> o.projectName().toLowerCase(Locale.ROOT)))
                .toList();
        record ChangedFile(Modpack.File oldFile, Modpack.File newFile) {}
        List<ChangedFile> changed = to.files().stream()
                .filter(file -> oldInfoBySlug.containsKey(file.projectSlug()))
                .filter(file -> !oldInfoBySlug.get(file.projectSlug()).fileId().equals(file.fileId()))
                .sorted(Comparator.comparing(o -> o.projectName().toLowerCase(Locale.ROOT)))
                .map(file -> new ChangedFile(oldInfoBySlug.get(file.projectSlug()), file))
                .toList();

        OutputTarget target = outputType.create();
        target.addHeader(from.title() + " - " + from.version() + " -> " + to.version());
        if (changedLoader) {
            target.addSubHeader(from.minecraft().loader() + " - " + from.minecraft().loaderVersion() + " -> " + to.minecraft().loaderVersion());
        }

        if (added.isEmpty() && removed.isEmpty() && changed.isEmpty()) {
            return target.result();
        }

        if (!added.isEmpty()) {
            target.addSubHeader("Added");
            target.beginList(false);
            for (Modpack.File file : added) {
                target.addListElement(target.formatLink(file.projectName()) + " (by " + target.formatLink(file.author()) + ")");
            }
            target.endList();
        }

        if (!removed.isEmpty()) {
            target.addSubHeader("Removed");
            target.beginList(false);
            for (Modpack.File file : removed) {
                target.addListElement(target.formatLink(file.projectName()) + " (by " + target.formatLink(file.author()) + ")");
            }
            target.endList();
        }

        if (!changed.isEmpty()) {
            target.addSubHeader("Changed");
            target.beginList(false);
            for (ChangedFile changedFile : changed) {
                String oldFile = target.formatLink(changedFile.oldFile.fileName());
                String newFile = target.formatLink(changedFile.newFile.fileName());
                target.addListElement(oldFile + " -> " + newFile);
            }
            target.endList();
        }

        return target.result();
    }
}
