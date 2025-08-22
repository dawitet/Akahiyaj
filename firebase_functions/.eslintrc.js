module.exports = {
  env: {
    es6: true,
    node: true
  },
  parserOptions: {
    ecmaVersion: 2018
  },
  extends: [
    "eslint:recommended"
  ],
  rules: {
    "no-restricted-globals": ["error", "name", "length"],
    "prefer-arrow-callback": "error",
    "quotes": ["error", "double", { "allowTemplateLiterals": true }],
    "object-curly-spacing": ["error", "always"],
    "max-len": ["error", { "code": 120 }],
    "comma-dangle": ["error", "never"],
    "no-unused-vars": ["error", { "argsIgnorePattern": "^_" }],
    "no-console": "off"
  },
  globals: {}
};
