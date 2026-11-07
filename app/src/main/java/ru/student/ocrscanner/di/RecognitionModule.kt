package ru.student.ocrscanner.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import ru.student.ocrscanner.recognition.RecognitionRepository
import ru.student.ocrscanner.recognition.RecognitionRepositoryImpl
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RecognitionModule {

    @Binds
    @Singleton
    abstract fun bindRecognitionRepository(
        impl: RecognitionRepositoryImpl
    ): RecognitionRepository
}