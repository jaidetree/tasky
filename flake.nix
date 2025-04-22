{
  description = "Tasky";

  inputs = {
    nixpkgs.url = "github:NixOS/nixpkgs/nixos-unstable";
    flake-utils.url = "github:numtide/flake-utils";
  };

  outputs = { self, nixpkgs, flake-utils }:
    flake-utils.lib.eachDefaultSystem (system:
      let
        pkgs = nixpkgs.legacyPackages.${system};
        notify =
          if pkgs.stdenv.isDarwin
          then pkgs.fswatch
          else pkgs.inotify-tools;
      in
      {
        devShells.default = pkgs.mkShell {
          buildInputs = with pkgs; [
            aider-chat

            elixir
            erlang
            lexical

            nodejs_23
            clj-kondo
            clojure
            clojure-lsp
            temurin-jre-bin-17

            # Optional but recommended development tools
            notify

            # PostgreSQL
            postgresql_14
            pgcli
            postgresql_14.lib
            openssl
          ];

          # Shell hook for additional environment setup
          shellHook = ''
            echo "Elixir development environment loaded!"
            echo "Elixir version: $(elixir --version)"
            echo "Postgres version: $(postgres --version)"
            echo "Node version: $(node --version)"
            echo "File watching tool: ${notify.name}"

            export PATH="./frontend/node_modubles/.bin:$PATH"
          '';
        };
      }
    );
}
