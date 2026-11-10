plugins {
    id("st.evening.mc.prelude.build") version "1.0.1"
    id("st.evening.kt.invokecontrol") version "2.4.20-0.1.0"
    id("maven-publish")
}

group = "st.evening.mc.xpk.storage"
version = "1.0.0"

preludeBuild {
    modId = "xpkstorage"
    mod {
        constantsClass = "XpkStorageConsts"
    }
    minecraft {
        mcVersion = "1.12.2"
        deobf {
            mappingsChannel = "stable"
            mappingsVersion = "39"
        }
    }
    presets {
        prelude {
            enable = true
        }
    }
}

repositories {
    maven {
        name = "Thiakil"
        url = uri("https://maven.thiakil.com/")
    }
    maven {
        name = "ModMaven"
        url = uri("https://modmaven.dev/")
    }
}

dependencies {
    api("st.evening.kt.invokecontrol:kt-invoke-control-lib:0.1.0")
    implementation(rfg.deobf("com.azanor.baubles:Baubles:1.12-1.5.2"))
    implementation(rfg.deobf("CraftTweaker2:CraftTweaker2-MC1120-Main:1.12-4.1.20.702"))
}
