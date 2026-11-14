# Comandi del fork (Flow YT). `make` da solo mostra l'elenco.
# Solo variante FOSS, come la build del fork (vedi FORK.md).

GRADLE      := ./gradlew
KEYSTORE    := release.keystore
KEY_ALIAS   := flow-yt
PROPS       := local.properties
SDK_DIR     := $(or $(shell sed -n 's/^sdk\.dir=//p' $(PROPS) 2>/dev/null),$(ANDROID_HOME),$(HOME)/Library/Android/sdk)
APKSIGNER   := $(lastword $(sort $(wildcard $(SDK_DIR)/build-tools/*/apksigner)))
RELEASE_DIR := app/build/outputs/apk/foss/release
DEBUG_DIR   := app/build/outputs/apk/foss/debug
# La Firestick è a 32 bit: questo è l'APK da installare lì.
TV_ABI      := armeabi-v7a

.DEFAULT_GOAL := help
.PHONY: help debug release test keystore check-signing verify sha1 apks clean

help: ## Mostra questo elenco
	@grep -E '^[a-z0-9-]+:.*## ' $(MAKEFILE_LIST) | awk -F':.*## ' '{printf "  make %-14s %s\n", $$1, $$2}'

debug: ## APK di debug (id .debug, lento: niente R8 né baseline profile)
	$(GRADLE) :app:assembleFossDebug
	@echo "APK per la Firestick: $(DEBUG_DIR)/app-foss-$(TV_ABI)-debug.apk"

release: check-signing ## APK di release firmato con la chiave del fork
	$(GRADLE) :app:assembleFossRelease
	@$(MAKE) --no-print-directory verify
	@echo "APK per la Firestick: $(RELEASE_DIR)/app-foss-$(TV_ABI)-release.apk"

test: ## Test unitari FOSS (gli stessi del workflow del fork)
	$(GRADLE) :app:testFossDebugUnitTest

keystore: ## Crea la chiave del fork (una volta sola; chiede le password)
	@if [ -f $(KEYSTORE) ]; then echo "$(KEYSTORE) esiste già: non lo sovrascrivo."; exit 1; fi
	keytool -genkeypair -v -keystore $(KEYSTORE) -alias $(KEY_ALIAS) -keyalg RSA -keysize 4096 -validity 10000
	@echo
	@echo "Fatto. Ora:"
	@echo "  1. salva $(KEYSTORE) e la password nel password manager;"
	@echo "  2. aggiungi a $(PROPS): storePassword=..., keyAlias=$(KEY_ALIAS), keyPassword=... (stessa password)."

check-signing: ## Controlla che keystore e password siano configurati (senza mostrarle)
	@test -f $(KEYSTORE) || { echo "Manca $(KEYSTORE) nella root: lancia 'make keystore'."; exit 1; }
	@for k in storePassword keyAlias keyPassword; do \
		grep -q "^$$k=." $(PROPS) 2>/dev/null || { echo "Manca '$$k=' in $(PROPS)."; exit 1; }; \
	done
	@echo "Firma configurata."

verify: ## Verifica la firma dell'APK di release per la Firestick
	@test -n "$(APKSIGNER)" || { echo "apksigner non trovato in $(SDK_DIR)/build-tools."; exit 1; }
	@test -f $(RELEASE_DIR)/app-foss-$(TV_ABI)-release.apk || { echo "Nessun APK di release: lancia 'make release'."; exit 1; }
	$(APKSIGNER) verify --print-certs $(RELEASE_DIR)/app-foss-$(TV_ABI)-release.apk | grep -E "certificate (DN|SHA-1 digest):"

sha1: ## Impronta SHA-1 della chiave del fork (per il client OAuth di Google)
	keytool -list -v -keystore $(KEYSTORE) -alias $(KEY_ALIAS) | grep -E "SHA1|SHA-1"

apks: ## Apre nel Finder la cartella degli APK
	@open app/build/outputs/apk/foss 2>/dev/null || echo "Nessun APK compilato."

clean: ## Pulisce la build
	$(GRADLE) clean
