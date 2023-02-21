package pl.memexurer.retpomusz.runclient;

import net.minecraftforge.gradle.user.TaskSingleReobf;
import org.gradle.api.Action;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.tasks.JavaExec;
import org.gradle.jvm.tasks.Jar;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class RunClientPlugin implements Plugin<Project> {
    @Override
    public void apply(Project project) {
        project.afterEvaluate(new Action<Project>() {
            @Override
            public void execute(Project project) {
                if (project.getState().getFailure() != null) {
                    return;
                }

                afterEvaluate(project);
            }
        });
    }

    private void afterEvaluate(Project project) {
        System.out.println("SIEMAAAAAAAAAAAAAAAAAAAA");
        JavaExec runClientExec = (JavaExec) project.getTasks().getByName("runClient");
        runClientExec.doFirst(new Action<Task>() {
            @Override
            public void execute(Task task) {
                runClientExec.setMain("Start");

                List<String> args = new ArrayList<>(runClientExec.getArgs());
                args.addAll(Arrays.asList(" --username k8s_ingress --version 1.8.8 --gameDir C:\\Users\\azeroy\\AppData\\Roaming\\.minecraft --assetsDir C:\\Users\\azeroy\\AppData\\Roaming\\.minecraft\\assets --assetIndex 1.8 --uuid e00d482d152f42f9b715f25037a97174 --accessToken eyJraWQiOiJhYzg0YSIsImFsZyI6IkhTMjU2In0.eyJ4dWlkIjoiMjUzNTQ1NTk0NDEyOTExMSIsImFnZyI6IkFkdWx0Iiwic3ViIjoiMDZlYWVmMTctOGYxOS00ZWZjLThiYzMtMTRkZTFmOTE3ZDRkIiwibmJmIjoxNjc2ODA5OTY1LCJhdXRoIjoiWEJPWCIsInJvbGVzIjpbXSwiaXNzIjoiYXV0aGVudGljYXRpb24iLCJleHAiOjE2NzY4OTYzNjUsImlhdCI6MTY3NjgwOTk2NSwicGxhdGZvcm0iOiJPTkVTVE9SRSIsInl1aWQiOiI2ZGJkYjg2ZWNjYmVlNmU0ODA5MDY2MTI1NjRhNGM1NyJ9.7jBtexHJItJMXHfmB3K5XVhQqjEl_0ZpJF0vF0sd5Q8 --userProperties {} --userType msa".split(" ")));
                runClientExec.setArgs(args);

                System.setProperty("log4j2.loggerContextFactory", "org.apache.logging.log4j.simple.SimpleLoggerContextFactory");
                System.setProperty("org.apache.logging.log4j.simplelog.level", "INFO");

           //     runClientExec.setJvmArgs(Arrays.asList("-javaagent:\"C:\\Users\\azeroy\\.gradle\\caches\\modules-2\\files-2.1\\org.spongepowered\\mixin\\0.7.11-SNAPSHOT\\7a670207bdb97db418118ad1e9bb42424ff3776d\\mixin-0.7.11-SNAPSHOT.jar\"", "-Dlog4j2.loggerContextFactory=org.apache.logging.log4j.simple.SimpleLoggerContextFactory", "-Dorg.apache.logging.log4j.simplelog.level=DEBUG"));

                runClientExec.setClasspath(
                        runClientExec.getClasspath().minus(
                                project.files(
                                      "C:\\Users\\azeroy\\.gradle\\caches\\minecraft\\net\\minecraft\\minecraft\\1.8.9\\stable\\20\\minecraftSrc-1.8.9.jar",
                                        "C:\\Users\\azeroy\\.gradle\\caches\\modules-2\\files-2.1\\net.minecraft\\launchwrapper\\1.11\\9c0592c6e1e9ea296a70948081bd4cc84dda1289\\launchwrapper-1.11.jar"
                               //         "C:\\Users\\azeroy\\IdeaProjects\\retpomusz-client\\client\\build\\libs\\client-1.0-SNAPSHOT.jar"
                                )
                        )
                );

                System.out.println(runClientExec.getClasspath().getFiles().stream().map(File::toString).collect(Collectors.joining("\n")));
            }
        });


    }
}
