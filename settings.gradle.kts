pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://api.mapbox.com/downloads/v2/releases/maven") } // Thêm repository Mapbox
        maven { url = uri("https://jitpack.io") } // Thêm nếu cần các thư viện từ JitPack
    }
}

rootProject.name = "DACS31" // Sửa tên dự án, bỏ dấu chấm
include(":app")