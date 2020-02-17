package io.github.liweitaos;

import io.github.liweitaos.embed.TomcatStarter;

/**
 * 嵌入式Tomcat容器
 *
 * @author liweitao
 * @date 2020-02-17 17:40:13
 */
public class TomcatEmbeddedApplication {

    public static void main(String[] args) {
        new TomcatStarter().start();
    }

}
