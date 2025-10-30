const path = require("path");
const TerserPlugin = require("terser-webpack-plugin");
const CssMinimizerPlugin = require("css-minimizer-webpack-plugin");
const MiniCssExtractPlugin = require("mini-css-extract-plugin");
const WarningsToErrorsPlugin = require("warnings-to-errors-webpack-plugin");

module.exports = (env, argv) => ({
    entry: {
        bundle: "./src/main/resources/js/app.js",
    },
    output: {
        path: path.resolve(__dirname, "./build/resources/main/static"),
        filename: "js/[name].js",
    },
    devtool: argv.mode === "production" ? false : "eval-source-map",
    performance: {
        maxEntrypointSize: 488000,
        maxAssetSize: 488000,
    },
    optimization: {
        minimize: true,
        minimizer: [new TerserPlugin(), new CssMinimizerPlugin()],
    },
    plugins: [
        new MiniCssExtractPlugin({
            filename: "css/[name].css",
        }),
        new WarningsToErrorsPlugin(),
    ],
    module: {
        rules: [
            {
                test: /\.js$/,
                use: {
                    loader: "babel-loader",
                    options: {
                        presets: ["@babel/preset-env"],
                    },
                },
            },
            {
                test: /\.scss$/,
                use: [
                    argv.mode === "production"
                        ? MiniCssExtractPlugin.loader
                        : "style-loader",
                    {
                        loader: "css-loader",
                        options: {
                            importLoaders: 1,
                            sourceMap: true,
                        },
                    },
                    {
                        loader: "postcss-loader",
                        options: {
                            postcssOptions: {
                                plugins: [require("autoprefixer")],
                            },
                            sourceMap: true,
                        },
                    },
                    {
                        loader: "sass-loader",
                        options: { sourceMap: true },
                    },
                ],
            },
        ],
    },
    resolve: {
        modules: [
            path.resolve(__dirname, "./src/main/resources"),
            "node_modules",
        ],
    },
    devServer: {
        port: 8081,
        compress: true,
        hot: true,
        static: false,
        watchFiles: [
            "src/main/resources/templates/**/*.html",
            "src/main/resources/js/**/*.js",
            "src/main/resources/scss/**/*.scss",
        ],
        proxy: [
            {
                context: "**",
                target: "http://localhost:8080",
                secure: false,
                prependPath: false,
                headers: {
                    "X-Devserver": "1",
                },
            },
        ],
    },
});
