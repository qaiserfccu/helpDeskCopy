package com.helpdesk.app.di

import com.helpdesk.app.BuildConfig
import com.helpdesk.app.data.api.AuthApi
import com.helpdesk.app.data.api.ReportsApi
import com.helpdesk.app.data.api.TicketsApi
import com.helpdesk.app.data.api.UsersApi
import com.helpdesk.app.data.repository.AuthRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Provider
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideAuthInterceptor(authRepositoryProvider: Provider<AuthRepository>): Interceptor {
        return Interceptor { chain ->
            val original = chain.request()
            
            // Skip auth header for auth endpoints
            val url = original.url.toString()
            if (url.contains("/auth/login") || 
                url.contains("/auth/register") || 
                url.contains("/auth/refresh")) {
                return@Interceptor chain.proceed(original)
            }

            val token = try {
                authRepositoryProvider.get().accessToken
            } catch (e: Exception) {
                null
            }

            if (token != null) {
                val request = original.newBuilder()
                    .header("Authorization", "Bearer $token")
                    .build()
                chain.proceed(request)
            } else {
                chain.proceed(original)
            }
        }
    }

    @Provides
    @Singleton
    fun provideLoggingInterceptor(): HttpLoggingInterceptor {
        return HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(
        authInterceptor: Interceptor,
        loggingInterceptor: HttpLoggingInterceptor
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideAuthApi(retrofit: Retrofit): AuthApi {
        return retrofit.create(AuthApi::class.java)
    }

    @Provides
    @Singleton
    fun provideTicketsApi(retrofit: Retrofit): TicketsApi {
        return retrofit.create(TicketsApi::class.java)
    }

    @Provides
    @Singleton
    fun provideReportsApi(retrofit: Retrofit): ReportsApi {
        return retrofit.create(ReportsApi::class.java)
    }

    @Provides
    @Singleton
    fun provideUsersApi(retrofit: Retrofit): UsersApi {
        return retrofit.create(UsersApi::class.java)
    }
}
