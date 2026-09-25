package pe.jamix.catalog.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

class PhotoSourcesTest {

    private static final Path IMG = Path.of("src/main/resources/static/img");

    private final PhotoSources photos = new PhotoSources();

    @Test
    void localPhotoGetsWebpVariants() {
        assertThat(photos.srcset("/img/falda-bobo.jpeg"))
                .isEqualTo("/img/opt/falda-bobo-480.webp 480w, /img/opt/falda-bobo-960.webp 960w");
        assertThat(photos.srcset("/img/hero.JPG")).startsWith("/img/opt/hero-480.webp");
    }

    @Test
    void otherUrlsKeepTheOriginalOnly() {
        assertThat(photos.srcset(null)).isNull();
        assertThat(photos.srcset("https://cdn.example.com/foto.jpeg")).isNull();
        assertThat(photos.srcset("/img/opt/falda-bobo-480.webp")).isNull();
        assertThat(photos.srcset("/img/logo.svg")).isNull();
    }

    /** Fails when a photo was added without running `npm run images` (the page would show broken images). */
    @Test
    void everyPhotoHasItsVariants() throws Exception {
        try (Stream<Path> files = Files.list(IMG)) {
            files.filter(Files::isRegularFile)
                    .map(file -> photos.srcset("/img/" + file.getFileName()))
                    .filter(srcset -> srcset != null)
                    .flatMap(srcset -> Stream.of(srcset.split(", ")))
                    .map(candidate -> IMG.resolve(candidate.substring("/img/".length(), candidate.indexOf(' '))))
                    .forEach(variant -> assertThat(variant)
                            .as("Falta %s: ejecuta `npm run images` en catalog-service", variant.getFileName())
                            .exists());
        }
    }
}
