var gulp = require('gulp');

var basePath = 'src/main/resources/assets/';
var targetPath = 'target/classes/assets';

gulp.task('html', function () {
  return gulp.src(basePath + '*.html')
    .pipe(gulp.dest(targetPath))
});

gulp.task('css', function () {
  return gulp.src(basePath + 'css/*.css')
    .pipe(gulp.dest(targetPath + '/css'))
});

gulp.task('js', function () {
  return gulp.src(basePath + 'js/*.js')
    .pipe(gulp.dest(targetPath + '/js'))
});

gulp.task('img', function () {
  return gulp.src(basePath + 'img/*.gif')
    .pipe(gulp.dest(targetPath + '/img'))
});

gulp.task('default', ['html', 'css', 'js', 'img']);

gulp.task('watch', function () {
  return gulp.watch(basePath + '**', ['html','css', 'js', 'img']);
});